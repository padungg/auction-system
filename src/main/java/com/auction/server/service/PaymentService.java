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
import com.auction.server.util.LockManager;
import com.auction.server.util.ValidationException;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý giao dịch thanh toán và phí nền tảng.
 */
public class PaymentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final AuctionMapper auctionMapper;

    // Phí nền tảng 2%
    public static final double PLATFORM_FEE_PERCENTAGE = 0.02;

    public PaymentService(AuctionDAO auctionDAO, UserDAO userDAO, ItemDAO itemDAO, AuctionMapper auctionMapper) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
        this.itemDAO = itemDAO;
        this.auctionMapper = auctionMapper;
    }

    /** Lấy danh sách phiên chờ thanh toán. */
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

    /** Lấy lịch sử thanh toán. */
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

    @FunctionalInterface
    private interface PaymentAction {
        Response execute() throws ValidationException;
    }

    private Response executeWithUserLocks(List<String> userIds, int index, PaymentAction action) throws ValidationException {
        if (index >= userIds.size()) {
            return action.execute();
        }
        Object lock = LockManager.getUserLock(userIds.get(index));
        synchronized (lock) {
            return executeWithUserLocks(userIds, index + 1, action);
        }
    }

    /** Kiểm tra tính hợp lệ trước khi thanh toán. */
    private Response validateAuctionForPayment(Auction auction, String userId) {
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy phiên đấu giá", null);
        }
        if (auction.getStatus() != AuctionStatus.FINISHED) {
            return new Response(ResponseStatus.BAD_REQUEST, "Phiên chưa kết thúc hoặc đã thanh toán rồi", null);
        }
        if (auction.getCurrentWinnerId() == null || !userId.trim().equals(auction.getCurrentWinnerId().trim())) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn không phải người thắng phiên này", null);
        }
        return null;
    }

    /** Thanh toán phiên đấu giá đã thắng. */
    public Response payAuction(String auctionId, String userId) throws ValidationException {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }

        Auction auction = auctionDAO.findById(auctionId);
        Response validation = validateAuctionForPayment(auction, userId);
        if (validation != null) {
            return validation;
        }

        // Tìm sellerId để khóa
        Item item = itemDAO.findById(auction.getItemId());
        final String sellerId = (item != null) ? item.getSellerId() : null;

        // Tìm adminId để khóa
        User admin = userDAO.findFirstByRole(com.auction.model.entity.UserRole.ADMIN);
        String adminId = (admin != null) ? admin.getId() : null;

        // Sắp xếp lock để tránh deadlock
        List<String> lockIds = new ArrayList<>();
        lockIds.add(userId);
        if (sellerId != null) lockIds.add(sellerId);
        if (adminId != null) lockIds.add(adminId);

        List<String> sortedLocks = lockIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        return executeWithUserLocks(sortedLocks, 0, () -> {
            // Re-fetch để đảm bảo nhất quán
            Auction lockedAuction = auctionDAO.findById(auctionId);
            Response lockedValidation = validateAuctionForPayment(lockedAuction, userId);
            if (lockedValidation != null) {
                return lockedValidation;
            }

            User user = userDAO.findById(userId);
            if (user == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
            }

            double basePrice = lockedAuction.getCurrentPrice();
            double platformFee = Math.round(basePrice * PLATFORM_FEE_PERCENTAGE * 100.0) / 100.0;
            double totalRequired = basePrice + platformFee;

            if (user.getBalance() < totalRequired) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Số dư không đủ! Cần: " + String.format("%,.0f", totalRequired)
                                + " VNĐ (gồm " + (PLATFORM_FEE_PERCENTAGE * 100) + "% phí) | Có: "
                                + String.format("%,.0f", user.getBalance()) + " VNĐ",
                        null);
            }

            // Giao dịch tài chính có rollback thủ công
            boolean txSuccess = processPaymentTransaction(user, basePrice, platformFee, totalRequired, lockedAuction.getItemId());
            if (!txSuccess) {
                return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể xử lý giao dịch tài chính trong Database", null);
            }

            // Cập nhật trạng thái PAID
            lockedAuction.setStatus(AuctionStatus.PAID);
            boolean auctionSuccess = auctionDAO.update(lockedAuction);
            if (!auctionSuccess) {
                LOGGER.error("PAY_FAILED: Không thể cập nhật trạng thái đấu giá thành PAID cho phiên {}. Đang hoàn tiền cho các bên...", auctionId);

                // Rollback buyer
                user.deposit(totalRequired);
                userDAO.update(user);

                // Rollback seller
                if (sellerId != null) {
                    User sellerObj = userDAO.findById(sellerId.trim());
                    if (sellerObj != null) {
                        sellerObj.withdraw(basePrice);
                        userDAO.update(sellerObj);
                    }
                }

                // Rollback admin
                if (adminId != null) {
                    User adminObj = userDAO.findById(adminId.trim());
                    if (adminObj != null) {
                        adminObj.withdraw(platformFee);
                        userDAO.update(adminObj);
                    }
                }

                // Rollback RAM
                lockedAuction.setStatus(AuctionStatus.FINISHED);
                return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể cập nhật trạng thái thanh toán của phiên đấu giá trong Database", null);
            }

            AuctionService.updateCachedStatus(auctionId, AuctionStatus.PAID.name());

            LOGGER.info("PAY_AUCTION: auctionId={} | buyer={} | giá={} | phí={} | tổng={} VNĐ",
                    auctionId, userId, String.format("%,.0f", basePrice), String.format("%,.0f", platformFee), String.format("%,.0f", totalRequired));
            return new Response(ResponseStatus.SUCCESS,
                    "Thanh toán thành công! (Đã trợ phí " + (PLATFORM_FEE_PERCENTAGE * 100) + "% = "
                            + String.format("%,.0f", platformFee) + " VNĐ)", null);
        });
    }

    /** Giao dịch có rollback thủ công nếu lỗi DB giữa chừng. */
    private boolean processPaymentTransaction(User buyer, double basePrice, double platformFee, double totalRequired, String itemId) {
        // Trừ tiền buyer
        buyer.withdraw(totalRequired);
        if (!userDAO.update(buyer)) {
            buyer.deposit(totalRequired); // rollback memory
            LOGGER.error("PAY_FAILED: Không thể trừ tiền buyer {} trong Database", buyer.getUsername());
            return false;
        }

        // Cộng tiền seller
        Item item = itemDAO.findById(itemId);
        User seller = null;
        if (item != null && item.getSellerId() != null) {
            seller = userDAO.findById(item.getSellerId().trim());
            if (seller != null) {
                seller.deposit(basePrice);
                if (!userDAO.update(seller)) {
                    LOGGER.error("PAY_FAILED: Không thể cộng tiền cho seller {} trong Database. Đang tiến hành rollback buyer...", seller.getUsername());
                    // Rollback buyer
                    buyer.deposit(totalRequired);
                    userDAO.update(buyer);
                    return false;
                }
                LOGGER.info("PAY: seller={} +{} VNĐ", seller.getUsername(), String.format("%,.0f", basePrice));
            }
        }

        // Cộng phí Admin
        User admin = userDAO.findFirstByRole(com.auction.model.entity.UserRole.ADMIN);
        if (admin != null) {
            admin.deposit(platformFee);
            if (!userDAO.update(admin)) {
                LOGGER.error("PAY_FAILED: Không thể cộng phí cho admin {} trong Database. Đang tiến hành rollback buyer và seller...", admin.getUsername());
                // Rollback seller
                if (seller != null) {
                    seller.withdraw(basePrice);
                    userDAO.update(seller);
                }
                // Rollback buyer
                buyer.deposit(totalRequired);
                userDAO.update(buyer);
                return false;
            }
            LOGGER.info("PAY: admin={} +{} VNĐ (phí {}%)", admin.getUsername(), String.format("%,.0f", platformFee), PLATFORM_FEE_PERCENTAGE * 100);
        } else {
            LOGGER.warn("Không tìm thấy Admin để nhận phí!");
        }

        return true;
    }
}