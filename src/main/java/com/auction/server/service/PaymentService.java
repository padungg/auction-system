package com.auction.server.service;

import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.Item;
import com.auction.model.entity.User;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.util.ValidationException;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý giao dịch thanh toán và tính phí nền tảng.
 */
public class PaymentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);
    
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final AuctionMapper auctionMapper;

    // Phí nền tảng  là 2%
    private static final double PLATFORM_FEE_PERCENTAGE = 0.02;

    public PaymentService(AuctionDAO auctionDAO, UserDAO userDAO, ItemDAO itemDAO, AuctionMapper auctionMapper) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
        this.itemDAO = itemDAO;
        this.auctionMapper = auctionMapper;
    }

    /**
     * Lấy danh sách phiên chờ thanh toán (user đã thắng nhưng chưa thanh toán).
     */
    public Response getPendingPayments(String userId) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        List<Auction> wonAuctions = auctionDAO.findByCurrentWinnerId(userId);
        List<AuctionSummaryDTO> pending = new ArrayList<>();
        for (Auction a : wonAuctions) {
            if (a.getStatus() == AuctionStatus.FINISHED) {
                pending.add(auctionMapper.toSummaryDTO(a));
            }
        }
        LOGGER.info("GET_PENDING_PAYMENTS: user={} | {} phiên", userId, pending.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy danh sách chờ thanh toán thành công", pending);
    }

    /**
     * Lấy lịch sử thanh toán (các phiên đã thanh toán).
     */
    public Response getPaymentHistory(String userId) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        List<Auction> wonAuctions = auctionDAO.findByCurrentWinnerId(userId);
        List<AuctionSummaryDTO> history = new ArrayList<>();
        for (Auction a : wonAuctions) {
            if (a.getStatus() == AuctionStatus.PAID) {
                history.add(auctionMapper.toSummaryDTO(a));
            }
        }
        LOGGER.info("GET_PAYMENT_HISTORY: user={} | {} phiên", userId, history.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy lịch sử thanh toán thành công", history);
    }

    /**
     * Thanh toán phiên đấu giá đã thắng.
     */
    public Response payAuction(String auctionId, String userId) throws ValidationException {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy phiên đấu giá", null);
        }
        if (auction.getStatus() != AuctionStatus.FINISHED) {
            return new Response(ResponseStatus.BAD_REQUEST, "Phiên chưa kết thúc hoặc đã thanh toán rồi", null);
        }
        if (!userId.equals(auction.getCurrentWinnerId())) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn không phải người thắng phiên này", null);
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
        }

        double basePrice = auction.getCurrentPrice();
        double platformFee = Math.round(basePrice * PLATFORM_FEE_PERCENTAGE * 100.0) / 100.0;
        double totalRequired = basePrice + platformFee;

        if (user.getBalance() < totalRequired) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Số dư không đủ! Cần: " + String.format("%,.0f", totalRequired)
                            + " VNĐ (gồm " + (PLATFORM_FEE_PERCENTAGE * 100) + "% phí) | Có: "
                            + String.format("%,.0f", user.getBalance()) + " VNĐ",
                    null);
        }

        // Thực hiện giao dịch tài chính
        processPaymentTransaction(user, basePrice, platformFee, totalRequired, auction.getItemId());

        // Chuyển status sang PAID
        auction.setStatus(AuctionStatus.PAID);
        auctionDAO.update(auction);

        LOGGER.info("PAY_AUCTION: auctionId={} | buyer={} | giá={} | phí={} | tổng={} VNĐ",
                auctionId, userId, String.format("%,.0f", basePrice), String.format("%,.0f", platformFee), String.format("%,.0f", totalRequired));
        return new Response(ResponseStatus.SUCCESS,
                "Thanh toán thành công! (Đã trợ phí " + (PLATFORM_FEE_PERCENTAGE * 100) + "% = "
                        + String.format("%,.0f", platformFee) + " VNĐ)", null);
    }

    /**
     * Xử lý giao dịch
     */
    private void processPaymentTransaction(User buyer, double basePrice, double platformFee, double totalRequired, String itemId) {
        // Trừ tiền buyer
        buyer.withdraw(totalRequired);
        userDAO.update(buyer);

        // Cộng tiền cho seller
        Item item = itemDAO.findById(itemId);
        if (item != null && item.getSellerId() != null) {
            User seller = userDAO.findById(item.getSellerId());
            if (seller != null) {
                seller.deposit(basePrice);
                userDAO.update(seller);
                LOGGER.info("PAY: seller={} +{} VNĐ", seller.getUsername(), String.format("%,.0f", basePrice));
            }
        }

        // Cộng phí cho Admin
        User admin = userDAO.findFirstByRole(com.auction.model.entity.UserRole.ADMIN);
        if (admin != null) {
            admin.deposit(platformFee);
            userDAO.update(admin);
            LOGGER.info("PAY: admin={} +{} VNĐ (phí {}%)", admin.getUsername(), String.format("%,.0f", platformFee), PLATFORM_FEE_PERCENTAGE * 100);
        } else {
            LOGGER.warn("Không tìm thấy Admin để nhận phí!");
        }
    }
}
