package com.auction.server.service;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.AutoBidEntry;
import com.auction.model.entity.BidTransaction;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.observer.AuctionManager;
import com.auction.server.util.AuctionUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý Auto-Bid (Đặt giá tự động)
 * - Thread-safe: Các method public được synchronized. Lock order: BidService -> AutoBidService.
 * - Anti-loop: Giới hạn MAX_ROUNDS = 50 lần tự động phản giá liên tiếp.
 */
public class AutoBidService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBidService.class);

    private static final int MAX_ROUNDS = 50;

    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final AutoBidDAO autoBidDAO;

    private final Map<String, PriorityQueue<AutoBidEntry>> registry = new ConcurrentHashMap<>();

    public AutoBidService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO, AutoBidDAO autoBidDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.autoBidDAO = autoBidDAO;
        loadFromDB();
    }

    private void loadFromDB() {
        List<AutoBidEntry> entries = autoBidDAO.findAll();
        for (AutoBidEntry entry : entries) {
            PriorityQueue<AutoBidEntry> queue = registry.computeIfAbsent(
                    entry.getAuctionId(), k -> new PriorityQueue<>());
            queue.add(entry);
        }
        LOGGER.info("Đã load {} auto-bids từ DB.", entries.size());
    }

    // ĐĂNG KÝ / HỦY AUTO-BID

    /**
     * Đăng ký Auto-Bid cho 1 phiên.
     *
     * Nếu user đã đăng ký cho phiên này → ghi đè (cập nhật maxBid/increment).
     * Thứ tự ưu tiên giữ nguyên theo registeredAt BAN ĐẦU (không reset khi ghi đè).
     */
    public synchronized Response register(AutoBidDTO dto, String userId) {
        if (dto == null || dto.getAuctionId() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin auto-bid", null);
        }
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        if (dto.getMaxBid() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Giá tối đa phải lớn hơn 0", null);
        }
        if (dto.getIncrement() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Bước tăng giá phải lớn hơn 0", null);
        }

        // Kiểm tra phiên còn đang mở không
        Auction auction = auctionDAO.findById(dto.getAuctionId());
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Chỉ có thể đăng ký auto-bid cho phiên đang mở", null);
        }
        if (dto.getMaxBid() <= auction.getCurrentPrice()) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Giá tối đa phải cao hơn giá hiện tại: "
                            + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ",
                    null);
        }

        // Lấy hoặc tạo queue cho phiên này
        PriorityQueue<AutoBidEntry> queue = registry.computeIfAbsent(
                dto.getAuctionId(), k -> new PriorityQueue<>());

        // Nếu user đã đăng ký → xóa entry cũ trước (để ghi đè)
        queue.removeIf(e -> e.getUserId().equals(userId));

        // Thêm entry mới
        AutoBidEntry newEntry = new AutoBidEntry(userId, dto.getAuctionId(), dto.getMaxBid(), dto.getIncrement());
        queue.add(newEntry);

        // Lưu vào DB
        autoBidDAO.save(newEntry);

        LOGGER.info("REGISTER: phiên={} | user={} | maxBid={} | increment={} | tổng auto-bidder={}",
                dto.getAuctionId(),
                userId,
                String.format("%,.0f", dto.getMaxBid()),
                String.format("%,.0f", dto.getIncrement()),
                queue.size());

        // Kích hoạt ngay lập tức: nếu maxBid > currentPrice + increment
        // đặt giá ngay mà không cần chờ người khác bid trước
        triggerAutoBids(dto.getAuctionId(), auction.getCurrentPrice(), auction.getCurrentWinnerId());

        return new Response(ResponseStatus.SUCCESS,
                "Đăng ký auto-bid thành công! Hệ thống đã bắt đầu tự động đặt giá cho bạn.", null);
    }

    /**
     * Hủy Auto-Bid của 1 user trong 1 phiên.
     */
    public synchronized Response cancel(String auctionId, String userId) {
        if (auctionId == null || userId == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin hủy auto-bid", null);
        }
        PriorityQueue<AutoBidEntry> queue = registry.get(auctionId);
        if (queue == null || queue.removeIf(e -> e.getUserId().equals(userId))) {
            if (queue != null && queue.isEmpty())
                registry.remove(auctionId);
            autoBidDAO.delete(auctionId, userId);
            LOGGER.info("CANCEL: phiên={} | user={}", auctionId, userId);
            return new Response(ResponseStatus.SUCCESS, "Đã hủy đăng ký auto-bid", null);
        }
        return new Response(ResponseStatus.NOT_FOUND, "Bạn chưa đăng ký auto-bid cho phiên này", null);
    }

    /**
     * Xóa toàn bộ auto-bid của 1 phiên khi phiên đóng.
     * Được gọi bởi AuctionService.closeAuction() và AuctionScheduler.
     */
    public synchronized void clearAuction(String auctionId) {
        registry.remove(auctionId);
        autoBidDAO.deleteByAuctionId(auctionId);
        LOGGER.info("CLEAR: đã xóa auto-bid của phiên={}", auctionId);
    }

    // KÍCH HOẠT AUTO-BID (gọi sau mỗi bid thành công)

    /**
     * Kích hoạt vòng tự động phản giá sau khi có bid mới.
     * Được gọi bởi BidService.placeBid() — đã nằm trong synchronized context.
     */
    public synchronized void triggerAutoBids(String auctionId,
                                             double startPrice,
                                             String startWinnerId) {
        PriorityQueue<AutoBidEntry> queue = registry.get(auctionId);
        if (queue == null || queue.isEmpty())
            return;

        double currentPrice = startPrice;
        String currentWinnerId = startWinnerId;
        int rounds = 0;
        boolean progress;

        do {
            progress = false;

            // Lấy snapshot + sắp xếp FCFS
            List<AutoBidEntry> sorted = new ArrayList<>(queue);
            Collections.sort(sorted);

            // Xác định entry bị loại (hết ngân sách)
            List<AutoBidEntry> exhausted = new ArrayList<>();
            AutoBidEntry winner = null;

            for (AutoBidEntry entry : sorted) {
                double nextBid = currentPrice + entry.getIncrement();

                if (nextBid > entry.getMaxBid()) {
                    // Hết ngân sách , đánh dấu xóa
                    exhausted.add(entry);
                    continue;
                }

                if (entry.getUserId().equals(currentWinnerId)) {
                    // Đang thắng rồi , bỏ qua (không cần bid thêm)
                    continue;
                }

                //  Người đầu tiên hợp lệ, có thể phản giá
                winner = entry;
                break;
            }

            // Xóa các entry hết ngân sách khỏi queue
            queue.removeAll(exhausted);
            if (!exhausted.isEmpty()) {
                for (AutoBidEntry e : exhausted) {
                    LOGGER.info("EXHAUSTED: user={} | maxBid={} vượt ngưỡng — loại khỏi queue",
                            e.getUserId(),
                            String.format("%,.0f", e.getMaxBid()));
                }
            }

            // Thực hiện auto-bid nếu tìm được winner
            if (winner != null) {
                double autoBidAmount = currentPrice + winner.getIncrement();

                // Persist vào DB
                Auction auction = auctionDAO.findById(auctionId);
                if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
                    break; // Phiên đã đóng giữa chừng → dừng
                }

                double oldPrice = currentPrice;
                currentPrice = autoBidAmount;
                currentWinnerId = winner.getUserId();

                auction.setCurrentPrice(currentPrice);
                auction.setCurrentWinnerId(currentWinnerId);
                auctionDAO.update(auction);

                // ANTI-SNIPING trong auto-bid
                // Nếu auto-bid xảy ra trong 60 giây cuối → gia hạn thêm 120 giây
                // Đảm bảo quy tắc anti-sniping áp dụng cho MỌI bid (thủ công hay tự động)
                AuctionUtils.applyAntiSnipe(auction, auctionDAO);

                // Lưu lịch sử BidTransaction — đánh dấu isAutoBid = true
                BidTransaction tx = new BidTransaction(
                        UUID.randomUUID().toString(),
                        currentWinnerId,
                        auctionId,
                        currentPrice,
                        LocalDateTime.now(),
                        true); // isAutoBid = true
                bidTransactionDAO.save(tx);

                String bidTimeIso = tx.getBidTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                // Thông báo realtime cho tất cả client đang xem phiên
                AuctionManager.getInstance().notifyBidUpdate(auctionId, currentPrice, currentWinnerId, bidTimeIso);

                LOGGER.info("AUTO_BID: phiên={} | user={} | {} → {} VNĐ",
                        auctionId,
                        currentWinnerId,
                        String.format("%,.0f", oldPrice),
                        String.format("%,.0f", currentPrice));

                progress = true;
            }

            rounds++;
        } while (progress && rounds < MAX_ROUNDS);

        if (rounds >= MAX_ROUNDS) {
            LOGGER.warn("MAX_ROUNDS reached tại phiên={} — dừng để tránh vòng lặp vô hạn", auctionId);
        }
    }
}