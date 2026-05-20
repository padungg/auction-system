package com.auction.server.util;

import com.auction.model.entity.Auction;
import com.auction.server.dao.AuctionDAO;

import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionUtils.class);
    private static final int SNIPE_THRESHOLD_SECONDS = 60;
    private static final int SNIPE_EXTENSION_SECONDS = 120;

    public static void applyAntiSnipe(Auction auction, AuctionDAO auctionDAO) {
        long secondsLeft = Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (secondsLeft > 0 && secondsLeft <= SNIPE_THRESHOLD_SECONDS) {
            auction.setEndTime(auction.getEndTime().plusSeconds(SNIPE_EXTENSION_SECONDS));
            auctionDAO.update(auction);
            LOGGER.info("ANTI-SNIPE: phiên={} | còn {}s → gia hạn thêm {}s",
                    auction.getId(), secondsLeft, SNIPE_EXTENSION_SECONDS);
        }
    }
}
