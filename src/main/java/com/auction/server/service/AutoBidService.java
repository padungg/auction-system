package com.auction.server.service;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.AutoBidEntry;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý Auto-Bid (Đặt giá tự động)
 * - Thread-safe: Các method public được synchronized. Lock order: BidService -> AutoBidService.
 * - Anti-loop: Giới hạn MAX_ROUNDS = 50 lần tự động phản giá liên tiếp.
 */
public class AutoBidService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBidService.class);

    private final AuctionDAO auctionDAO;
    private final AutoBidDAO autoBidDAO;
    private BidService bidService;

    private final Map<String, PriorityQueue<AutoBidEntry>> registry = new ConcurrentHashMap<>();

    public AutoBidService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO) {
        this.auctionDAO = auctionDAO;
        this.autoBidDAO = autoBidDAO;
        loadFromDB();
    }

    public void setBidService(BidService bidService) {
        this.bidService = bidService;
    }

    private void loadFromDB() {
        List<AutoBidEntry> entries = autoBidDAO.findAll();
        for (AutoBidEntry entry : entries) {
            PriorityQueue<AutoBidEntry> queue = registry.computeIfAbsent(
                    entry.getAuctionId(), ignored -> new PriorityQueue<>());
            queue.add(entry);
        }
        LOGGER.info("Đã load {} auto-bids từ DB.", entries.size());
    }

    // ĐĂNG KÝ / HỦY AUTO-BID

    /**
     * Đăng ký Auto-Bid cho 1 phiên.
     * Nếu user đã đăng ký cho phiên này → ghi đè (cập nhật maxBid/increment).
     * Thứ tự ưu tiên giữ nguyên theo registeredAt BAN ĐẦU (không reset khi ghi đè).
     */
    public Response register(AutoBidDTO dto, String userId) {
        if (dto == null || dto.getAuctionId() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin auto-bid", null);
        }

        Object lock = com.auction.server.util.LockManager.getAuctionLock(dto.getAuctionId());
        synchronized (lock) {
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
            if (dto.getIncrement() < auction.getStepPrice()) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Bước tăng giá phải lớn hơn hoặc bằng bước giá tối thiểu của phiên: "
                                + String.format("%,.0f", auction.getStepPrice()) + " VNĐ",
                        null);
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
                    dto.getAuctionId(), ignored -> new PriorityQueue<>());

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
            if (bidService != null) {
                bidService.runAutoBids(dto.getAuctionId());
            }

            return new Response(ResponseStatus.SUCCESS,
                    "Đăng ký auto-bid thành công! Hệ thống đã bắt đầu tự động đặt giá cho bạn.", null);
        }
    }

    /**
     * Hủy Auto-Bid của 1 user trong 1 phiên.
     */
    public Response cancel(String auctionId, String userId) {
        if (auctionId == null || userId == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin hủy auto-bid", null);
        }

        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
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
    }

    /**
     * Xóa toàn bộ auto-bid của 1 phiên khi phiên đóng.
     * Được gọi bởi AuctionService.closeAuction() và AuctionScheduler.
     */
    public void clearAuction(String auctionId) {
        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            registry.remove(auctionId);
            autoBidDAO.deleteByAuctionId(auctionId);
            LOGGER.info("CLEAR: đã xóa auto-bid của phiên={}", auctionId);
        }
    }

    // KÍCH HOẠT AUTO-BID (gọi sau mỗi bid thành công)

    /**
     * Kích hoạt vòng tự động phản giá sau khi có bid mới.
     * Được gọi bởi BidService.placeBid() — đã nằm trong synchronized context.
     */
    public record NextAutoBid(String userId, double bidAmount) {}

    /**
     * Tính toán xem ai là người auto-bid tiếp theo và giá là bao nhiêu.
     * Không chạm vào DB hay gửi Notification (đảm bảo SRP).
     * BidService sẽ gọi hàm này và tự thực hiện vòng lặp lưu DB.
     */
    public NextAutoBid calculateNextAutoBid(String auctionId, double currentPrice, String currentWinnerId) {
        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            PriorityQueue<AutoBidEntry> queue = registry.get(auctionId);
            if (queue == null || queue.isEmpty())
                return null;

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

                // Người đầu tiên hợp lệ, có thể phản giá
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

            if (winner != null) {
                return new NextAutoBid(winner.getUserId(), currentPrice + winner.getIncrement());
            }

            return null;
        }
    }
}