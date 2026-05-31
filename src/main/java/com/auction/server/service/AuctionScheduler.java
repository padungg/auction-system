package com.auction.server.service;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.server.dao.AuctionDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trình lên lịch tự động mở/đóng phiên đấu giá.
 */
public class AuctionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionScheduler.class);
    private static final int INTERVAL_SECONDS = 30;

    private final AuctionDAO auctionDAO;
    private final AuctionService auctionService;
    private ScheduledExecutorService scheduler;

    public AuctionScheduler(AuctionDAO auctionDAO, AuctionService auctionService) {
        this.auctionDAO = auctionDAO;
        this.auctionService = auctionService;
    }



    /** Khởi động scheduler. */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionScheduler-Thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::checkExpiredAuctions,
                0,
                INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        LOGGER.info("START: kiểm tra mỗi {}s", INTERVAL_SECONDS);
    }

    /** Dừng scheduler an toàn. */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    LOGGER.warn("FORCE_STOP: Task bị hủy sau 10s");
                } else {
                    LOGGER.info("STOP: Dừng an toàn");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }



    private void checkExpiredAuctions() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Kích hoạt phiên OPEN đến giờ
            List<Auction> openAuctions = auctionDAO.findAllByStatus(AuctionStatus.OPEN);
            int startedCount = 0;
            for (Auction auction : openAuctions) {
                if (!now.isBefore(auction.getStartTime())) {
                    Object lock = com.auction.server.util.LockManager.getAuctionLock(auction.getId());
                    synchronized (lock) {
                        try {
                            Auction currentAuction = auctionDAO.findById(auction.getId());
                            if (currentAuction != null && currentAuction.getStatus() == AuctionStatus.OPEN) {
                                currentAuction.setStatus(AuctionStatus.RUNNING);
                                boolean success = auctionDAO.update(currentAuction);
                                if (success) {
                                    AuctionService.updateCachedStatus(currentAuction.getId(), AuctionStatus.RUNNING.name());
                                    startedCount++;
                                    LOGGER.info("SCHEDULER: Phiên đấu giá {} bắt đầu hoạt động (RUNNING).", currentAuction.getId());
                                } else {
                                    currentAuction.setStatus(AuctionStatus.OPEN); // Rollback memory
                                    LOGGER.error("SCHEDULER: Không thể cập nhật trạng thái RUNNING cho phiên {} trong Database.", currentAuction.getId());
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Lỗi khi chuyển trạng thái phiên đấu giá sang RUNNING: {}", auction.getId(), e);
                        }
                    }
                }
            }
            if (startedCount > 0) {
                LOGGER.info("CHECK: Đã kích hoạt {} phiên đấu giá mới", startedCount);
            }

            // Đóng phiên RUNNING hết hạn
            List<Auction> runningAuctions = auctionDAO.findAllByStatus(AuctionStatus.RUNNING);
            int closedCount = 0;
            for (Auction auction : runningAuctions) {
                if (now.isAfter(auction.getEndTime())) {
                    try {
                        auctionService.closeAuction(auction.getId());
                        closedCount++;
                    } catch (Exception e) {
                        LOGGER.error("Lỗi khi đóng phiên hết hạn: {}", auction.getId(), e);
                    }
                }
            }

            if (closedCount > 0) {
                LOGGER.info("CHECK: Đã đóng {} phiên hết hạn", closedCount);
            }

        } catch (Exception e) {
            LOGGER.error("ERROR khi kiểm tra phiên đấu giá", e);
        }
    }
}
