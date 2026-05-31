package com.auction.server.service;

import com.auction.model.dto.BidRequestDTO;
import com.auction.model.dto.MyBidHistoryDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.BidTransaction;
import com.auction.model.entity.Item;
import com.auction.model.entity.User;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.observer.AuctionManager;
import com.auction.server.util.AuctionUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ xử lý đặt giá (Bid) và đồng bộ real-time.
 */
public class BidService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BidService.class);
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final AutoBidService autoBidService;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;

    public BidService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO,
                      AutoBidService autoBidService, ItemDAO itemDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.autoBidService = autoBidService;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
    }

    /** Đặt giá. */
    public Response placeBid(BidRequestDTO dto, String bidderId) {

        if (dto == null || dto.getAuctionId() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đặt giá", null);
        }
        if (bidderId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        if (dto.getBidAmount() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Số tiền đặt giá phải lớn hơn 0", null);
        }
        
        String auctionId = dto.getAuctionId();
        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);

        synchronized (lock) {
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return new Response(ResponseStatus.BAD_REQUEST, "Phiên đấu giá hiện đang " + auction.getStatus(), null);
            }
            if (LocalDateTime.now().isAfter(auction.getEndTime())) {

                auction.setStatus(AuctionStatus.FINISHED);
                boolean success = auctionDAO.update(auction);
                if (!success) {
                    auction.setStatus(AuctionStatus.RUNNING); // rollback in memory
                    return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể đóng phiên đấu giá đã hết hạn trong Database", null);
                }
                AuctionService.updateCachedStatus(auctionId, AuctionStatus.FINISHED.name());
                
                // Dọn AutoBid queue
                autoBidService.clearAuction(auctionId);
                
                // Thông báo đóng phiên ngay lập tức
                AuctionManager.getInstance().notifyAuctionClosed(
                        auction.getId(), auction.getCurrentPrice(), auction.getCurrentWinnerId());
                return new Response(ResponseStatus.BAD_REQUEST, "Phiên đấu giá đã kết thúc thời gian", null);
            }

            // Không cho chủ sản phẩm tự đặt giá
            Item item = itemDAO.findById(auction.getItemId().trim());
            if (item != null && bidderId.trim().equals(item.getSellerId().trim())) {
                return new Response(ResponseStatus.BAD_REQUEST, "Bạn không thể đặt giá cho sản phẩm của chính mình!", null);
            }


            double bidAmount = dto.getBidAmount();
            double minRequiredBid;
            if (auction.getCurrentWinnerId() == null) {
                minRequiredBid = auction.getCurrentPrice();
                if (bidAmount < minRequiredBid) {
                    return new Response(ResponseStatus.BAD_REQUEST,
                            "Giá đặt phải lớn hơn hoặc bằng giá khởi điểm: " + String.format("%,.0f", minRequiredBid) + " VNĐ", null);
                }
            } else {
                minRequiredBid = auction.getCurrentPrice() + auction.getStepPrice();
                if (bidAmount < minRequiredBid) {
                    return new Response(ResponseStatus.BAD_REQUEST,
                            "Giá đặt tối thiểu phải là " + String.format("%,.0f", minRequiredBid) + " VNĐ (Giá hiện tại + Bước giá tối thiểu " + String.format("%,.0f", auction.getStepPrice()) + " VNĐ)", null);
                }
            }
            if (auction.getCurrentWinnerId() != null && bidderId.trim().equals(auction.getCurrentWinnerId().trim())) {
                return new Response(ResponseStatus.BAD_REQUEST, "Bạn đang là người đặt giá cao nhất, không cần bid thêm!",
                        null);
            }

            boolean success = applyBid(auction, bidderId, bidAmount, false);
            if (!success) {
                return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể lưu giá đặt mới vào Database", null);
            }

            runAutoBids(auctionId);

            return new Response(ResponseStatus.SUCCESS,
                    "Đặt giá thành công! Giá mới: " + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ", auction.getCurrentPrice());
        }
    }

    /** Kích hoạt đặt giá tự động (Auto-Bid). */
    public void runAutoBids(String auctionId) {
        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

            int rounds = 0;
            while (rounds < 50) {
                AutoBidService.NextAutoBid nextBid = autoBidService.calculateNextAutoBid(auctionId, auction.getCurrentPrice(), auction.getCurrentWinnerId());
                if (nextBid == null) break;

                // Dừng nếu hết thời gian đấu giá
                if (LocalDateTime.now().isAfter(auction.getEndTime())) break;

                boolean success = applyBid(auction, nextBid.userId(), nextBid.bidAmount(), true);
                if (!success) break;
                rounds++;
            }

            if (rounds >= 50) {
                LOGGER.warn("MAX_ROUNDS reached tại phiên={} — dừng để tránh vòng lặp vô hạn", auctionId);
            }
        }
    }

    /** Cập nhật thông tin phiên, lưu lịch sử và gửi thông báo. */
    private boolean applyBid(Auction auction, String bidderId, double bidAmount, boolean isAutoBid) {
        double oldPrice = auction.getCurrentPrice();
        String oldWinnerId = auction.getCurrentWinnerId();

        auction.setCurrentPrice(bidAmount);
        auction.setCurrentWinnerId(bidderId);
        
        if (!auctionDAO.update(auction)) {
            // Rollback memory
            auction.setCurrentPrice(oldPrice);
            auction.setCurrentWinnerId(oldWinnerId);
            LOGGER.error("PLACE_BID_FAILED: Không thể cập nhật giá phiên đấu giá {} trong Database", auction.getId());
            return false;
        }

        BidTransaction transaction = new BidTransaction(
                UUID.randomUUID().toString(),
                bidderId,
                auction.getId(),
                bidAmount,
                LocalDateTime.now(),
                isAutoBid);
        if (!bidTransactionDAO.save(transaction)) {
            // DB trúng thầu nhưng lỗi ghi lịch sử
            LOGGER.error("PLACE_BID_WARNING: Không thể lưu lịch sử bid của bidder {} tại phiên {} vào Database", bidderId, auction.getId());
        }

        String bidTimeIso = transaction.getBidTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String bidderName = bidderId;
        if (userDAO != null) {
            try {
                User bidderUser = userDAO.findById(bidderId);
                if (bidderUser != null && bidderUser.getUsername() != null) {
                    bidderName = bidderUser.getUsername();
                }
            } catch (Exception ignored) {}
        }

        String itemName = "một sản phẩm";
        try {
            Item item = itemDAO.findById(auction.getItemId());
            if (item != null && item.getName() != null) {
                itemName = item.getName();
            }
        } catch (Exception ignored) {}

        boolean isExtended = AuctionUtils.applyAntiSnipe(auction, auctionDAO);
        
        String newEndTimeStr = isExtended && auction.getEndTime() != null ? auction.getEndTime().toString() : null;
        
        int newBidCount = 0;
        try {
            newBidCount = bidTransactionDAO.findByAuctionId(auction.getId()).size();
        } catch (Exception e) {
            LOGGER.error("Lỗi khi lấy số lượt bid của phiên {}", auction.getId(), e);
        }
        LocalDateTime newEndTime = isExtended ? auction.getEndTime() : null;
        AuctionService.syncCacheOnBid(auction.getId(), bidAmount, bidderId, newBidCount, newEndTime);

        AuctionManager.getInstance().notifyBidUpdate(auction.getId(), bidAmount, bidderId, bidderName, itemName, bidTimeIso, newEndTimeStr);

        LOGGER.info("{}BID: phiên={} | bidder={} | {} → {} VNĐ",
                isAutoBid ? "AUTO_" : "PLACE_",
                auction.getId(),
                bidderId,
                String.format("%,.0f", oldPrice),
                String.format("%,.0f", bidAmount));
        return true;
    }

    /** Lấy lịch sử bid của phiên đấu giá. */
    public Response getBidHistory(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }
        List<BidTransaction> history = bidTransactionDAO.findByAuctionId(auctionId.trim());
        // Sắp xếp theo thời gian
        history.sort((a, b) -> a.getBidTime().compareTo(b.getBidTime()));

        LOGGER.info("GET_HISTORY: phiên={} | {} bản ghi", auctionId, history.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy lịch sử bid thành công", history);
    }

    /** Lấy lịch sử bid cá nhân. */
    public Response getMyBidHistory(String bidderId) {
        if (bidderId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        List<BidTransaction> txList = bidTransactionDAO.findByBidderId(bidderId);
        List<MyBidHistoryDTO> result = new ArrayList<>();

        for (BidTransaction tx : txList) {
            Auction auction = auctionDAO.findById(tx.getAuctionId());
            String itemName = tx.getAuctionId();
            if (auction != null) {
                Item item = itemDAO.findById(auction.getItemId());
                if (item != null) itemName = item.getName();
            }

            String status;
            if (auction == null) {
                status = "Không rõ";
            } else if (auction.getStatus() == AuctionStatus.RUNNING ||
                    auction.getStatus() == AuctionStatus.OPEN) {
                status = "Đang đấu";
            } else if (bidderId.equals(auction.getCurrentWinnerId())) {
                status = "Thắng";
            } else {
                status = "Thất bại";
            }

            result.add(new MyBidHistoryDTO(
                    tx.getAuctionId(), itemName, tx.getBidAmount(), tx.getBidTime(), status
            ));
        }

        LOGGER.info("GET_MY_HISTORY: user={} | {} bản ghi", bidderId, result.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy lịch sử bid cá nhân thành công", result);
    }
}
