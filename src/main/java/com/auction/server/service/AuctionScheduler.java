package com.auction.server.service;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.observer.AuctionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler tự động đóng phiên đấu giá đã hết hạn.
 *
 * Cơ chế:
 *   - Chạy ngầm (background thread) mỗi INTERVAL_SECONDS giây
 *   - Quét tất cả phiên đang OPENING
 *   - Phiên nào có endTime < now → đóng + notify Observer
 *
 * Vòng đời:
 *   ServerApp.main():
 *     scheduler.start()   ← bắt đầu khi server khởi động
 *     ...
 *     scheduler.stop()    ← dừng khi server tắt
 *
 * Thread-safety:
 *   - ScheduledExecutorService quản lý thread pool 1 thread
 *   - Không cần synchronized thêm vì chỉ 1 thread gọi checkExpired()
 */
public class AuctionScheduler {

    /** Chu kỳ kiểm tra: 30 giây/lần */
    private static final int INTERVAL_SECONDS = 30;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AuctionDAO auctionDAO;
    private ScheduledExecutorService scheduler;

    public AuctionScheduler(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    // ════════════════════════════════════════════════════════════════
    // VÒNG ĐỜI
    // ════════════════════════════════════════════════════════════════

    /**
     * Khởi động scheduler — gọi 1 lần khi ServerApp.main() chạy.
     *
     * scheduleAtFixedRate(task, initialDelay, period, unit):
     *   - initialDelay = 0  : chạy ngay lập tức lần đầu
     *   - period = 30       : sau đó cứ 30 giây chạy lại
     */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionScheduler-Thread");
            t.setDaemon(true); // tự tắt khi JVM tắt, không cần gọi stop() thủ công
            return t;
        });
        // initialDelay = 0: chạy ngay khi start → tự động đóng các phiên đã hết hạn từ trước lần restart
        // → giải quyết trường hợp server crash giữa chừng rồi restart lại
        scheduler.scheduleAtFixedRate(
                this::checkExpiredAuctions,
                0,
                INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        System.out.println("[AuctionScheduler] START: kiểm tra mỗi " + INTERVAL_SECONDS + "s");
    }

    /**
     * Dừng scheduler — gọi khi server tắt (Ctrl+C hoặc ServerApp shutdown hook).
     *
     * awaitTermination(10s):
     *   - Không kill thread ngay lập tức.
     *   - Chờ tối đa 10 giây để task đang chạy (checkExpiredAuctions) hoàn thành.
     *   - Sau 10s mà chưa xong → mới force-stop.
     *   → Đảm bảo không bị cắt giữa chừng khi đang update DB.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown(); // Dừng nhận task mới
            try {
                // Chờ tối đa 10s để task hiện tại hoàn thành
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow(); // Quá 10s → force stop
                    System.out.println("[AuctionScheduler] FORCE_STOP: task bị hủy sau 10s");
                } else {
                    System.out.println("[AuctionScheduler] STOP: dừng sạch, không có task bị cắt");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // LOGIC KIỂM TRA
    // ════════════════════════════════════════════════════════════════

    /**
     * Quét toàn bộ phiên OPENING → đóng những phiên đã hết hạn.
     *
     * Được gọi tự động mỗi INTERVAL_SECONDS giây bởi scheduler.
     *
     * Luồng:
     *   1. Lấy danh sách phiên OPENING
     *   2. Với mỗi phiên: so sánh endTime với now
     *   3. Nếu quá hạn → CLOSED + update DB + notify Observer
     */
    private void checkExpiredAuctions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Auction> openingAuctions = auctionDAO.findAllByStatus(AuctionStatus.RUNNING);

            int closedCount = 0;
            for (Auction auction : openingAuctions) {
                if (now.isAfter(auction.getEndTime())) {
                    closeExpiredAuction(auction, now);
                    closedCount++;
                }
            }

            // Chỉ log nếu có phiên bị đóng (tránh spam log mỗi 30s)
            if (closedCount > 0) {
                System.out.println("[AuctionScheduler] CHECK " + now.format(FMT)
                        + ": đóng " + closedCount + " phiên hết hạn");
            }

        } catch (Exception e) {
            // Bắt mọi exception để scheduler không bị dừng đột ngột
            System.out.println("[AuctionScheduler] ERROR: " + e.getMessage());
        }
    }

    /**
     * Đóng 1 phiên đã hết hạn và thông báo realtime cho client.
     */
    private void closeExpiredAuction(Auction auction, LocalDateTime now) {
        // Bước 1: Update DB TRƯỚC — đảm bảo dữ liệu nhất quán
        // Nếu sau bước này server crash → restart lại vẫn đọc được CLOSED từ DB
        auction.setStatus(AuctionStatus.FINISHED);
        auctionDAO.update(auction);

        // Bước 2: Notify sau khi DB đã an toàn
        // Dùng notifyAuctionClosed() (không phải notifyBidUpdate) để client phân biệt
        // được đây là sự kiện đóng phiên → khóa form bid, hiển thị "Phiên đã kết thúc"
        AuctionManager.getInstance().notifyAuctionClosed(
                auction.getId(),
                auction.getCurrentPrice(),
                auction.getCurrentWinnerId()
        );

        System.out.println("[AuctionScheduler] CLOSE: auctionId=" + auction.getId()
                + " | hết hạn lúc=" + auction.getEndTime().format(FMT)
                + " | giá cuối=" + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ"
                + " | winner=" + (auction.getCurrentWinnerId() != null
                                  ? auction.getCurrentWinnerId() : "Không có"));
    }
}
