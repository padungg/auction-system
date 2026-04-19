package com.auction.server.service;

import com.auction.model.dto.BidRequestDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.BidTransaction;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.observer.AuctionManager;
import com.auction.server.strategy.AutoBidStrategy;
import com.auction.server.strategy.BidStrategy;
import com.auction.server.strategy.NormalBidStrategy;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý nghiệp vụ Đặt Giá (Bid).
 *
 * Áp dụng 3 Design Pattern:
 *   - SYNCHRONIZED: Khóa method → tránh race condition khi nhiều người bid cùng lúc
 *   - STRATEGY: Chọn chiến lược bid (Normal / Auto) dựa trên bidType
 *   - OBSERVER: Sau khi bid thành công → thông báo cho tất cả client đang xem phiên đó
 *
 * Tính năng bổ sung:
 *   - Anti-Sniping: Nếu bid trong 2 phút cuối → kéo dài thêm 2 phút
 *   - Lịch sử bid: Trả về danh sách BidTransaction của 1 phiên
 */
public class BidService {

    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;

    // STRATEGY PATTERN: Map chứa các chiến lược bid
    private final Map<String, BidStrategy> strategies;

    public BidService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;

        // Đăng ký các chiến lược
        this.strategies = new HashMap<>();
        this.strategies.put("NORMAL", new NormalBidStrategy());
        this.strategies.put("AUTO", new AutoBidStrategy());
    }

    /**
     * Xử lý đặt giá — SYNCHRONIZED để tránh race condition.
     *
     * Luồng:
     *   1. Validation cơ bản (null check, kiểm tra login)
     *   2. Tìm auction + kiểm tra trạng thái/thời gian
     *   3. STRATEGY: Chọn chiến lược → validate → tính giá thực tế
     *   4. Cập nhật giá + winner
     *   5. ANTI-SNIPING: Kéo dài nếu bid cuối giờ
     *   6. Lưu lịch sử BidTransaction
     *   7. OBSERVER: Thông báo cho tất cả client đang xem
     */
    public synchronized Response placeBid(BidRequestDTO dto, String bidderId) {
        // ========== 1. VALIDATION CƠ BẢN ==========
        if (dto == null || dto.getAuctionId() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đặt giá", null);
        }

        if (bidderId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }

        if (dto.getBidAmount() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Số tiền đặt giá phải lớn hơn 0", null);
        }

        // ========== 2. TÌM PHIÊN ĐẤU GIÁ ==========
        Auction auction = auctionDAO.findById(dto.getAuctionId());
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }

        // Kiểm tra trạng thái phiên
        if (auction.getStatus() != AuctionStatus.OPENING) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Phiên đấu giá chưa mở hoặc đã kết thúc (trạng thái: " + auction.getStatus() + ")", null);
        }

        // Kiểm tra thời gian
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            auction.setStatus(AuctionStatus.CLOSED);
            auctionDAO.update(auction);
            return new Response(ResponseStatus.BAD_REQUEST, "Phiên đấu giá đã hết thời gian!", null);
        }

        // ========== 3. STRATEGY PATTERN: Chọn chiến lược ==========
        String bidType = (dto.getBidType() != null) ? dto.getBidType().toUpperCase() : "NORMAL";
        BidStrategy strategy = strategies.getOrDefault(bidType, strategies.get("NORMAL"));

        // Xác định số tiền cần validate (Auto-Bid dùng maxBidAmount)
        double amountToValidate = bidType.equals("AUTO") ? dto.getMaxBidAmount() : dto.getBidAmount();

        // Gọi strategy validate
        Response validationError = strategy.validate(auction, amountToValidate, bidderId);
        if (validationError != null) {
            return validationError; // Strategy trả lỗi → dừng
        }

        // Tính giá bid thực tế (Auto-Bid có thể tính khác giá gửi lên)
        double actualBid = strategy.calculateActualBid(auction, amountToValidate);

        // ========== 4. CẬP NHẬT GIÁ VÀ WINNER ==========
        double oldPrice = auction.getCurrentPrice();
        auction.setCurrentPrice(actualBid);
        auction.setCurrentWinnerId(bidderId);
        auctionDAO.update(auction);

        // ========== 5. ANTI-SNIPING: Kéo dài nếu bid trong 2 phút cuối ==========
        LocalDateTime antiSnipeThreshold = auction.getEndTime().minusMinutes(2);
        if (LocalDateTime.now().isAfter(antiSnipeThreshold)) {
            auction.setEndTime(auction.getEndTime().plusMinutes(2));
            auctionDAO.update(auction);
            System.out.println(">>> [Anti-Snipe] Kéo dài thêm 2 phút cho phiên " + auction.getId());
        }

        // ========== 6. LƯU LỊCH SỬ GIAO DỊCH ==========
        BidTransaction transaction = new BidTransaction(
                UUID.randomUUID().toString(),
                bidderId,
                dto.getAuctionId(),
                actualBid,
                LocalDateTime.now()
        );
        bidTransactionDAO.save(transaction);

        // ========== 7. OBSERVER: Thông báo cho tất cả client đang xem ==========
        AuctionManager.getInstance().notifyBidUpdate(dto.getAuctionId(), actualBid, bidderId);

        // ========== LOG + TRẢ KẾT QUẢ ==========
        System.out.println(">>> [BidService] Bid thành công! Phiên: " + dto.getAuctionId()
                + " | Kiểu: " + bidType
                + " | Giá cũ: " + String.format("%,.0f", oldPrice)
                + " → Giá mới: " + String.format("%,.0f", actualBid));

        return new Response(ResponseStatus.SUCCESS,
                "Đặt giá thành công! Giá mới: " + String.format("%,.0f", actualBid) + " VNĐ", null);
    }

    /**
     * Lấy lịch sử bid của 1 phiên đấu giá.
     */
    public Response getBidHistory(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }

        // Kiểm tra phiên tồn tại
        Auction auction = auctionDAO.findById(auctionId.trim());
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }

        // Lấy danh sách lịch sử bid
        List<BidTransaction> history = bidTransactionDAO.findByAuctionId(auctionId.trim());

        System.out.println(">>> [BidService] Trả về " + history.size() + " lịch sử bid cho phiên " + auctionId);
        return new Response(ResponseStatus.SUCCESS, "Lấy lịch sử bid thành công", history);
    }
}
