package com.auction.server.service;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.BidTransaction;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.observer.AuctionManager;
import com.auction.server.util.AuctionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * AUTO-BID SERVICE — Đặt giá tự động thay người dùng khi có bid mới từ đối thủ.
 *
 * Cơ chế:
 * 1. User gửi REGISTER_AUTO_BID {auctionId, maxBid, increment}
 * → AutoBidEntry được lưu vào PriorityQueue của phiên đó.
 * 2. Sau mỗi lần PLACE_BID thành công, BidService gọi triggerAutoBids()
 * → AutoBidService duyệt queue, tự động phản giá cho các user đã đăng ký.
 * 3. Vòng lặp tiếp tục cho đến khi không còn ai có thể phản giá.
 *
 * Ưu tiên FCFS (First Come First Served):
 * - PriorityQueue<AutoBidEntry> sắp xếp theo registeredAt (sớm nhất = ưu tiên
 * cao).
 * - Khi nhiều người cùng có thể phản giá, người đăng ký trước được xét trước.
 *
 * Thread-safety:
 * - Tất cả public method đều synchronized.
 * - triggerAutoBids() được gọi từ BidService.placeBid() đã synchronized
 * → không có deadlock (lock order luôn là BidService → AutoBidService).
 *
 * Giới hạn vòng lặp:
 * - MAX_ROUNDS = 50 để ngăn vòng lặp vô hạn trong trường hợp cực đoan.
 */
public class AutoBidService {

    /**
     * Giới hạn số vòng tự động phản giá liên tiếp — phòng trường hợp nhiều người
     * auto-bid
     */
    private static final int MAX_ROUNDS = 50;

    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final AutoBidDAO autoBidDAO;

    /**
     * Map: auctionId → PriorityQueue<AutoBidEntry> (FCFS — sắp xếp theo
     * registeredAt)
     * ConcurrentHashMap cho phép đọc đồng thời an toàn; write vẫn cần synchronized.
     */
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
        System.out.println("[AutoBidService] Đã load " + entries.size() + " auto-bids từ DB.");
    }

    // ════════════════════════════════════════════════════════════════
    // ĐĂNG KÝ / HỦY AUTO-BID
    // ════════════════════════════════════════════════════════════════

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

        System.out.println("[AutoBidService] REGISTER: phiên=" + dto.getAuctionId()
                + " | user=" + userId
                + " | maxBid=" + String.format("%,.0f", dto.getMaxBid())
                + " | increment=" + String.format("%,.0f", dto.getIncrement())
                + " | tổng auto-bidder=" + queue.size());

        return new Response(ResponseStatus.SUCCESS,
                "Đăng ký auto-bid thành công! Hệ thống sẽ tự động phản giá cho bạn.", null);
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
            System.out.println("[AutoBidService] CANCEL: phiên=" + auctionId + " | user=" + userId);
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
        System.out.println("[AutoBidService] CLEAR: đã xóa auto-bid của phiên=" + auctionId);
    }

    // ════════════════════════════════════════════════════════════════
    // KÍCH HOẠT AUTO-BID (gọi sau mỗi bid thành công)
    // ════════════════════════════════════════════════════════════════

    /**
     * Kích hoạt vòng tự động phản giá sau khi có bid mới.
     *
     * Được gọi bởi BidService.placeBid() — đã nằm trong synchronized context.
     *
     * Thuật toán (FCFS):
     * 1. Lấy toàn bộ AutoBidEntry của phiên, sắp xếp theo registeredAt.
     * 2. Xóa các entry đã hết ngân sách (maxBid < currentPrice + increment).
     * 3. Tìm entry đầu tiên (đăng ký sớm nhất) KHÔNG phải current winner
     * và có maxBid >= currentPrice + increment.
     * 4. Tự động đặt giá: newPrice = currentPrice + increment.
     * 5. Persist DB + notify Observer + lưu BidTransaction.
     * 6. Lặp lại từ bước 2 cho đến khi không còn ai phản giá được.
     *
     * @param auctionId     ID phiên đấu giá
     * @param startPrice    Giá hiện tại (sau bid vừa xảy ra)
     * @param startWinnerId Winner hiện tại (người vừa bid)
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

            // ── Bước 1: Lấy snapshot + sắp xếp FCFS ────────────────
            List<AutoBidEntry> sorted = new ArrayList<>(queue);
            Collections.sort(sorted); // AutoBidEntry.compareTo → registeredAt tăng dần

            // ── Bước 2: Xác định entry bị loại (hết ngân sách) ────
            List<AutoBidEntry> exhausted = new ArrayList<>();
            AutoBidEntry winner = null;

            for (AutoBidEntry entry : sorted) {
                double nextBid = currentPrice + entry.getIncrement();

                if (nextBid > entry.getMaxBid()) {
                    // Hết ngân sách → đánh dấu xóa
                    exhausted.add(entry);
                    continue;
                }

                if (entry.getUserId().equals(currentWinnerId)) {
                    // Đang thắng rồi → bỏ qua (không cần bid thêm)
                    continue;
                }

                // ← Người đầu tiên (FCFS) hợp lệ, có thể phản giá
                winner = entry;
                break;
            }

            // Xóa các entry hết ngân sách khỏi queue
            queue.removeAll(exhausted);
            if (!exhausted.isEmpty()) {
                for (AutoBidEntry e : exhausted) {
                    System.out.println("[AutoBidService] EXHAUSTED: user=" + e.getUserId()
                            + " | maxBid=" + String.format("%,.0f", e.getMaxBid())
                            + " vượt ngưỡng — loại khỏi queue");
                }
            }

            // ── Bước 3: Thực hiện auto-bid nếu tìm được winner ─────
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

                // ── ANTI-SNIPING trong auto-bid ───────────────────────────────────
                // Nếu auto-bid xảy ra trong 60 giây cuối → gia hạn thêm 120 giây
                // Đảm bảo quy tắc anti-sniping áp dụng cho MỌI bid (thủ công hay tự động)
                AuctionUtils.applyAntiSnipe(auction, auctionDAO);

                // Lưu lịch sử BidTransaction
                BidTransaction tx = new BidTransaction(
                        UUID.randomUUID().toString(),
                        currentWinnerId,
                        auctionId,
                        currentPrice,
                        LocalDateTime.now());
                bidTransactionDAO.save(tx);

                String bidTimeIso = tx.getBidTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                // Thông báo realtime cho tất cả client đang xem phiên
                AuctionManager.getInstance().notifyBidUpdate(auctionId, currentPrice, currentWinnerId, bidTimeIso);

                System.out.println("[AutoBidService] AUTO_BID: phiên=" + auctionId
                        + " | user=" + currentWinnerId
                        + " | " + String.format("%,.0f", oldPrice)
                        + " → " + String.format("%,.0f", currentPrice) + " VNĐ");

                progress = true; // Có bid xảy ra → kiểm tra lại từ đầu
            }

            rounds++;
        } while (progress && rounds < MAX_ROUNDS);

        if (rounds >= MAX_ROUNDS) {
            System.out.println("[AutoBidService] MAX_ROUNDS reached tại phiên=" + auctionId
                    + " — dừng để tránh vòng lặp vô hạn");
        }
    }
}
