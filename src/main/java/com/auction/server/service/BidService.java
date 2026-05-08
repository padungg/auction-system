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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ Đặt Giá (Bid).
 *   - SYNCHRONIZED: Khóa method → tránh race condition khi nhiều người bid cùng lúc
 *   - OBSERVER: Sau khi bid thành công → thông báo cho tất cả client đang xem phiên đó
 *   - AUTO-BID: Sau mỗi bid, kích hoạt AutoBidService để xử lý các lượt tự động phản giá
 *   - Lịch sử bid: Trả về danh sách BidTransaction của 1 phiên
 */
public class BidService {
    private final AuctionDAO         auctionDAO;
    private final BidTransactionDAO  bidTransactionDAO;
    private final AutoBidService     autoBidService;   // xử lý auto-bid sau mỗi bid

    public BidService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO,
                      AutoBidService autoBidService) {
        this.auctionDAO        = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.autoBidService    = autoBidService;
    }
    /**
     * Xử lý đặt giá — SYNCHRONIZED để tránh race condition.
     *   2. Tìm auction + kiểm tra trạng thái/thời gian
     *   3. Cập nhật giá + winner
     *   4. Lưu lịch sử BidTransaction
     *   5. OBSERVER: Thông báo cho tất cả client đang xem
     */
    public synchronized Response placeBid(BidRequestDTO dto, String bidderId){
        // Validation cơ bản (null check, kiểm tra login)
        if (dto == null || dto.getAuctionId() == null){
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đặt giá", null);
        }
        if (bidderId == null){
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        if (dto.getBidAmount() <= 0){
            return new Response(ResponseStatus.BAD_REQUEST, "Số tiền đặt giá phải lớn hơn 0", null);
        }
        // Tìm phiên đấu giá
        Auction auction = auctionDAO.findById(dto.getAuctionId());
        if (auction == null){
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }
        if (auction.getStatus() != AuctionStatus.RUNNING){
            return new Response(ResponseStatus.BAD_REQUEST, "Phiên đấu giá hiện đang " + auction.getStatus(), null);
        }
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            // Cập nhật DB trước để đảm bảo nhất quán dữ liệu
            auction.setStatus(AuctionStatus.FINISHED);
            auctionDAO.update(auction);
            // Notify ngay lập tức — không chờ AuctionScheduler (có thể chậm tới 30 giây)
            // → Tất cả client đang xem phiên này sẽ nhận sự kiện "AUCTION_CLOSED" ngay
            AuctionManager.getInstance().notifyAuctionClosed(
                    auction.getId(), auction.getCurrentPrice(), auction.getCurrentWinnerId());
            return new Response(ResponseStatus.BAD_REQUEST, "Phiên đấu giá đã kết thúc thời gian", null);
        }
        // Xử lý đặt giá
        double bidAmount = dto.getBidAmount();
        if (bidAmount <= auction.getCurrentPrice()){
            return new Response(ResponseStatus.BAD_REQUEST, "Giá bid phải cao hơn giá hiện tại: " + auction.getCurrentPrice(), null);
        }
        if (bidderId.equals(auction.getCurrentWinnerId())){
            return new Response(ResponseStatus.BAD_REQUEST, "Bạn đang là người đặt giá cao nhất, không cần bid thêm!", null);
        }
        // Cập nhập giá và winner
        double oldPrice = auction.getCurrentPrice();
        auction.setCurrentPrice(bidAmount);
        auction.setCurrentWinnerId(bidderId);
        auctionDAO.update(auction);
        // Lịch sử giao dịch
        BidTransaction transaction = new BidTransaction(
                UUID.randomUUID().toString(),
                bidderId,
                dto.getAuctionId(),
                bidAmount,
                LocalDateTime.now());
        bidTransactionDAO.save(transaction); // Lưu lịch sử bid

        String bidTimeIso = transaction.getBidTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // ── ANTI-SNIPING ──────────────────────────────────────────────────────
        // Nếu bid diễn ra trong 60 giây cuối → gia hạn thêm 120 giây
        // Mục đích: ngăn “sniper” đặt giá vào đúng giây chót để thắng mà không ai kịp phản đứng
        long secondsLeft = Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (secondsLeft > 0 && secondsLeft <= 60) {
            auction.setEndTime(auction.getEndTime().plusSeconds(120));
            auctionDAO.update(auction);
            System.out.println("[BidService] ANTI-SNIPE: Phiên " + dto.getAuctionId()
                    + " còn " + secondsLeft + "s → Gia hạn thêm 120 giây");
        }
        // ── OBSERVER: push realtime cho tất cả client đang xem phiên này ────
        AuctionManager.getInstance().notifyBidUpdate(dto.getAuctionId(), bidAmount, bidderId, bidTimeIso);

        System.out.println("[BidService] PLACE_BID: phiên=" + dto.getAuctionId()
                + " | bidder=" + bidderId
                + " | " + String.format("%,.0f", oldPrice) + " → " + String.format("%,.0f", bidAmount) + " VNĐ");

        // ── AUTO-BID: kích hoạt các lượt tự động phản giá đã đăng ký ────────────────
        // Gọi SAU khi bid của bidder đã được lưu vào DB và notify xong
        // AutoBidService sẽ tự loop cho đến khi không còn ai phản giá được
        autoBidService.triggerAutoBids(dto.getAuctionId(), bidAmount, bidderId);

        return new Response(ResponseStatus.SUCCESS,
                "Đặt giá thành công! Giá mới: " + String.format("%,.0f", bidAmount) + " VNĐ", bidAmount);
    }
    /**
     * Lấy lịch sử bid của 1 phiên đấu giá.
     */
    public Response getBidHistory(String auctionId){
        if (auctionId == null || auctionId.trim().isEmpty()){
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null){
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }
        // Lấy danh sách lịch sử Bid
        List<BidTransaction> history = bidTransactionDAO.findByAuctionId(auctionId.trim());
        // 3.2.5 Sắp xếp tăng dần theo thời gian (trục X) để phục vụ cho Line Chart
        history.sort((a, b) -> a.getBidTime().compareTo(b.getBidTime()));
        
        System.out.println("[BidService] GET_HISTORY: phiên=" + auctionId + " | " + history.size() + " bản ghi");
        return new Response(ResponseStatus.SUCCESS, "Lấy lịch sử bid thành công", history);
    }
}
