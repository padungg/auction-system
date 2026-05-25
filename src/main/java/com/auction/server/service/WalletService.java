package com.auction.server.service;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.User;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.util.LockManager;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chuyên trách xử lý nghiệp vụ Ví tiền (Wallet):
 * Nạp tiền, Rút tiền, Cập nhật hồ sơ.
 */
public class WalletService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    /** Phí nền tảng 2% — tham chiếu từ PaymentService */
    private static final double PLATFORM_FEE = PaymentService.PLATFORM_FEE_PERCENTAGE;

    private final UserDAO userDAO;
    private final AuctionDAO auctionDAO;
    private final UserMapper userMapper;

    public WalletService(UserDAO userDAO, AuctionDAO auctionDAO, UserMapper userMapper) {
        this.userDAO = userDAO;
        this.auctionDAO = auctionDAO;
        this.userMapper = userMapper;
    }

    /**
     * Nạp tiền vào tài khoản.
     */
    public Response deposit(String userId, double amount) {
        return processTransaction(userId, amount, true);
    }

    /**
     * Rút tiền khỏi tài khoản.
     */
    public Response withdraw(String userId, double amount) {
        return processTransaction(userId, amount, false);
    }

    /**
     * Lấy thông tin tài khoản (để refresh số dư).
     */
    public Response getMyProfile(String userId) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Chưa đăng nhập", null);
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
        }
        return new Response(ResponseStatus.SUCCESS, "OK", userMapper.toFullDTO(user));
    }

    /**
     * Cập nhật hồ sơ cá nhân (thread-safe).
     */
    public Response updateProfile(String userId, Map<String, String> payload) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Chưa đăng nhập", null);
        }

        Object lock = LockManager.getUserLock(userId);
        synchronized (lock) {
            User user = userDAO.findById(userId);
            if (user == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
            }

            String fullName  = payload.getOrDefault("fullName", "").trim();
            String phone     = payload.getOrDefault("phone", "").trim();
            String address   = payload.getOrDefault("address", "").trim();
            String storeName = payload.getOrDefault("storeName", "").trim();
            String password  = payload.getOrDefault("password", "").trim();

            if (fullName.isEmpty()) {
                return new Response(ResponseStatus.BAD_REQUEST, "Họ và tên không được để trống", null);
            }

            user.setFullName(fullName);
            user.setPhone(phone);
            user.setAddress(address);
            user.setStoreName(storeName);
            if (!password.isEmpty()) {
                user.setPassword(password);
            }

            boolean success = userDAO.update(user);
            if (!success) {
                return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể cập nhật thông tin hồ sơ trong Database", null);
            }
            LOGGER.info("UPDATE_PROFILE: user={} | fullName={}", user.getUsername(), fullName);
            return new Response(ResponseStatus.SUCCESS, "Cập nhật hồ sơ thành công!", userMapper.toFullDTO(user));
        }
    }

        // PRIVATE

    private Response processTransaction(String userId, double amount, boolean isDeposit) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        if (amount <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Số tiền phải lớn hơn 0", null);
        }

        Object lock = LockManager.getUserLock(userId);
        synchronized (lock) {
            User user = userDAO.findById(userId);
            if (user == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
            }

            if (isDeposit) {
                user.deposit(amount);
            } else {
                // Tính tổng tiền đang bị "giữ lại" cho các phiên thắng chưa thanh toán
                double reservedAmount = calculateReservedAmount(userId);

                double availableBalance = user.getBalance() - reservedAmount;

                if (amount > availableBalance) {
                    String msg = "Số dư khả dụng để rút: " + String.format("%,.0f", Math.max(availableBalance, 0)) + " VNĐ";
                    if (reservedAmount > 0) {
                        msg += " (đang giữ " + String.format("%,.0f", reservedAmount)
                                + " VNĐ cho phiên đấu giá chưa thanh toán)";
                    }
                    return new Response(ResponseStatus.BAD_REQUEST, msg, null);
                }
                user.withdraw(amount);
            }

            boolean success = userDAO.update(user);
            if (!success) {
                // Khôi phục bộ nhớ RAM
                if (isDeposit) {
                    user.withdraw(amount);
                } else {
                    user.deposit(amount);
                }
                return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể cập nhật số dư trong Database", null);
            }

            String action = isDeposit ? "DEPOSIT" : "WITHDRAW";
            String sign   = isDeposit ? "+" : "-";
            String msg    = isDeposit ? "Nạp tiền thành công! Số dư: " : "Rút tiền thành công! Số dư: ";

            LOGGER.info("{}: user={} | {}{} VNĐ | balance={} VNĐ",
                    action,
                    user.getUsername(),
                    sign,
                    String.format("%,.0f", amount),
                    String.format("%,.0f", user.getBalance()));

            return new Response(ResponseStatus.SUCCESS,
                    msg + String.format("%,.0f", user.getBalance()) + " VNĐ",
                    user.getBalance());
        }
    }

    /**
     * Tính tổng số tiền bị "giữ lại" cho các phiên thắng nhưng chưa thanh toán.
     * Mỗi phiên cần: giá hiện tại + 2% phí nền tảng.
     */
    private double calculateReservedAmount(String userId) {
        List<Auction> wonAuctions = auctionDAO.findByCurrentWinnerId(userId);
        double reserved = 0.0;
        for (Auction auction : wonAuctions) {
            if (auction.getStatus() == AuctionStatus.FINISHED) {
                double price = auction.getCurrentPrice();
                double fee   = Math.round(price * PLATFORM_FEE * 100.0) / 100.0;
                reserved += price + fee;
            }
        }
        return reserved;
    }
}
