package com.auction.server.service;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.observer.AuctionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trình lên lịch chạy ngầm để tự động đóng các phiên đấu giá hết hạn.
 */
public class AuctionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionScheduler.class);
    private static final int INTERVAL_SECONDS = 30;

    private final AuctionDAO auctionDAO;
    private ScheduledExecutorService scheduler;

    public AuctionScheduler(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    // VÒNG ĐỜI

    /**
     * Khởi động scheduler.
     */
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

    /**
     * Dừng scheduler an toàn khi server tắt.
     */
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

    // LOGIC KIỂM TRA

    /**
     * Quét các phiên đang chạy và đóng phiên đã hết hạn.
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

            if (closedCount > 0) {
                LOGGER.info("CHECK: Đã đóng {} phiên hết hạn", closedCount);
            }

        } catch (Exception e) {
            LOGGER.error("ERROR khi kiểm tra phiên hết hạn", e);
        }
    }

    /**
     * Đóng phiên đấu giá hết hạn và thông báo cho client.
     */
    private void closeExpiredAuction(Auction auction, LocalDateTime now) {
        auction.setStatus(AuctionStatus.FINISHED);
        auctionDAO.update(auction);

        AuctionManager.getInstance().notifyAuctionClosed(
                auction.getId(),
                auction.getCurrentPrice(),
                auction.getCurrentWinnerId()
        );

        LOGGER.info("CLOSE: auctionId={} | end={} | finalPrice={} VNĐ | winner={}",
                auction.getId(),
                auction.getEndTime(),
                String.format("%,.0f", auction.getCurrentPrice()),
                auction.getCurrentWinnerId() != null ? auction.getCurrentWinnerId() : "Không có");
    }
}
