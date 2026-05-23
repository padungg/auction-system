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
 * Dịch vụ xử lý Đặt Giá (Bid).
 * - Thread-safe (Synchronized): Ngăn chặn race condition khi đặt giá.
 * - Realtime (Observer): Thông báo lập tức cho các client đang xem.
 * - Tích hợp AutoBid và lưu trữ lịch sử giao dịch.
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

    /**
     * Xử lý đặt giá.
     * Sử dụng fine-grained locking theo auctionId thay vì khóa toàn bộ service.
     */
    public Response placeBid(BidRequestDTO dto, String bidderId) {
        // Validation cơ bản (null check, kiểm tra login)
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
            // Tìm phiên đấu giá
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return new Response(ResponseStatus.BAD_REQUEST, "Phiên đấu giá hiện đang " + auction.getStatus(), null);
            }
            if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                // Cập nhật DB trước để đảm bảo nhất quán dữ liệu
                auction.setStatus(AuctionStatus.FINISHED);
                auctionDAO.update(auction);
                
                // Dọn dẹp AutoBid queue để tránh Memory Leak
                autoBidService.clearAuction(auctionId);
                
                // Notify ngay lập tức — không chờ AuctionScheduler (có thể chậm tới 30 giây)
                AuctionManager.getInstance().notifyAuctionClosed(
                        auction.getId(), auction.getCurrentPrice(), auction.getCurrentWinnerId());
                return new Response(ResponseStatus.BAD_REQUEST, "Phiên đấu giá đã kết thúc thời gian", null);
            }
            // Xử lý đặt giá
            double bidAmount = dto.getBidAmount();
            double minRequiredBid;
            if (auction.getCurrentWinnerId() == null) {
                minRequiredBid = auction.getCurrentPrice(); // startingPrice
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
            if (bidderId.equals(auction.getCurrentWinnerId())) {
                return new Response(ResponseStatus.BAD_REQUEST, "Bạn đang là người đặt giá cao nhất, không cần bid thêm!",
                        null);
            }

            // Cập nhập giá và winner cho Bid thủ công
            applyBid(auction, bidderId, bidAmount, false);

            // AUTO-BID
            runAutoBids(auctionId);

            return new Response(ResponseStatus.SUCCESS,
                    "Đặt giá thành công! Giá mới: " + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ", auction.getCurrentPrice());
        }
    }

    /**
     * Vòng lặp kích hoạt các lượt đặt giá tự động (Auto-Bid).
     */
    public void runAutoBids(String auctionId) {
        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

            int rounds = 0;
            while (rounds < 50) { // 50 là MAX_ROUNDS
                AutoBidService.NextAutoBid nextBid = autoBidService.calculateNextAutoBid(auctionId, auction.getCurrentPrice(), auction.getCurrentWinnerId());
                if (nextBid == null) break;

                // Nếu AutoBid đẩy giá lên sau khi phiên đã đóng thời gian
                if (LocalDateTime.now().isAfter(auction.getEndTime())) break;

                applyBid(auction, nextBid.userId(), nextBid.bidAmount(), true);
                rounds++;
            }

            if (rounds >= 50) {
                LOGGER.warn("MAX_ROUNDS reached tại phiên={} — dừng để tránh vòng lặp vô hạn", auctionId);
            }
        }
    }

    /**
     * Cập nhật thông tin phiên, lưu lịch sử và bắn thông báo.
     * Dùng chung cho cả đặt giá thủ công và tự động (Đảm bảo DRY).
     */
    private void applyBid(Auction auction, String bidderId, double bidAmount, boolean isAutoBid) {
        double oldPrice = auction.getCurrentPrice();
        auction.setCurrentPrice(bidAmount);
        auction.setCurrentWinnerId(bidderId);
        auctionDAO.update(auction);

        // Lịch sử giao dịch
        BidTransaction transaction = new BidTransaction(
                UUID.randomUUID().toString(),
                bidderId,
                auction.getId(),
                bidAmount,
                LocalDateTime.now(),
                isAutoBid);
        bidTransactionDAO.save(transaction); // Lưu lịch sử bid

        String bidTimeIso = transaction.getBidTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Lấy tên người đặt giá và tên sản phẩm để gửi thông báo đầy đủ
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

        // ANTI-SNIPING
        AuctionUtils.applyAntiSnipe(auction, auctionDAO);
        // OBSERVER
        AuctionManager.getInstance().notifyBidUpdate(auction.getId(), bidAmount, bidderId, bidderName, itemName, bidTimeIso);

        LOGGER.info("{}BID: phiên={} | bidder={} | {} → {} VNĐ",
                isAutoBid ? "AUTO_" : "PLACE_",
                auction.getId(),
                bidderId,
                String.format("%,.0f", oldPrice),
                String.format("%,.0f", bidAmount));
    }

    /**
     * Lấy lịch sử bid của 1 phiên đấu giá.
     */
    public Response getBidHistory(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }
        // Lấy danh sách lịch sử Bid
        List<BidTransaction> history = bidTransactionDAO.findByAuctionId(auctionId.trim());
        // Sắp xếp tăng dần theo thời gian (trục X) để phục vụ cho Line Chart
        history.sort((a, b) -> a.getBidTime().compareTo(b.getBidTime()));

        LOGGER.info("GET_HISTORY: phiên={} | {} bản ghi", auctionId, history.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy lịch sử bid thành công", history);
    }

    /**
     * Lấy lịch sử bid cá nhân của user (tất cả các phiên đã tham gia).
     * Trả về MyBidHistoryDTO có itemName và result (Thắng/Thất bại/Đang đấu).
     */
    public Response getMyBidHistory(String bidderId) {
        if (bidderId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        List<BidTransaction> txList = bidTransactionDAO.findByBidderId(bidderId);
        List<MyBidHistoryDTO> result = new ArrayList<>();

        for (BidTransaction tx : txList) {
            Auction auction = auctionDAO.findById(tx.getAuctionId());
            String itemName = tx.getAuctionId(); // fallback
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
