package com.auction.server.service;

import com.auction.model.dto.AuctionDetailDTO;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.Item;
import com.auction.model.entity.ItemFactory;
import com.auction.model.entity.User;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.observer.AuctionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ liên quan đến Phiên Đấu Giá.
 *
 * Chức năng:
 *   - getAllAuctions()    : Lấy danh sách phiên đang mở (trang chủ)
 *   - getAuctionDetail() : Xem chi tiết 1 phiên
 *   - createAuction()    : Tạo phiên mới — dùng FACTORY PATTERN để tạo Item
 *   - closeAuction()     : Đóng phiên thủ công
 */
public class AuctionService {
    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
    }

    /**
     * Lấy danh sách tất cả phiên đấu giá đang OPENING.
     * Trả về dạng rút gọn (AuctionSummaryDTO) để tối ưu tốc độ mạng.
     */
    public Response getAllAuctions() {
        List<Auction> auctions = auctionDAO.findAllByStatus(AuctionStatus.OPENING);
        List<AuctionSummaryDTO> summaryList = new ArrayList<>();
        for (Auction auction : auctions) {
            Item item = itemDAO.findById(auction.getItemId());
            String itemName = (item != null) ? item.getName() : "Không rõ";
            AuctionSummaryDTO summary = new AuctionSummaryDTO(
                    auction.getId(),
                    itemName,
                    auction.getCurrentPrice(),
                    auction.getStatus().name()
            );
            summaryList.add(summary);
        }
        System.out.println("[AuctionService] GET_ALL: " + summaryList.size() + " phiên đang mở");
        return new Response(ResponseStatus.SUCCESS, "Lấy danh sách thành công", summaryList);
    }

    /**
     * Lấy chi tiết đầy đủ của 1 phiên đấu giá.
     * Bao gồm: thông tin item, tên seller, tên winner, thời gian, giá...
     */
    public Response getAuctionDetail(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }
        Auction auction = auctionDAO.findById(auctionId.trim());
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy phiên đấu giá", null);
        }
        Item item = itemDAO.findById(auction.getItemId());

        String sellerName = "Không rõ";
        if (item != null && item.getSellerId() != null) {
            User seller = userDAO.findById(item.getSellerId());
            if (seller != null) sellerName = seller.getFullName();
        }

        String winnerName = "Chưa có";
        if (auction.getCurrentWinnerId() != null) {
            User winner = userDAO.findById(auction.getCurrentWinnerId());
            if (winner != null) winnerName = winner.getFullName();
        }

        AuctionDetailDTO detail = new AuctionDetailDTO();
        detail.setAuctionId(auction.getId());
        detail.setItemName(item != null ? item.getName() : "Không rõ");
        detail.setItemDetails(item != null ? item.getDetailInfo() : "");
        detail.setDescription(item != null ? item.getDescription() : "");
        detail.setStartingPrice(item != null ? item.getStartingPrice() : 0);
        detail.setCurrentPrice(auction.getCurrentPrice());
        detail.setSellerName(sellerName);
        detail.setCurrentWinnerName(winnerName);
        detail.setStartTime(auction.getStartTime());
        detail.setEndTime(auction.getEndTime());
        detail.setStatus(auction.getStatus().name());
        return new Response(ResponseStatus.SUCCESS, "Lấy chi tiết phiên thành công", detail);
    }

    /**
     * Tạo phiên đấu giá mới.
     *
     * FACTORY PATTERN: Dùng ItemFactory để tạo đúng loại Item
     *   theo itemType (ELECTRONICS / ART / VEHICLE).
     *
     * Luồng:
     *   1. Validate input
     *   2. ItemFactory.createXxx() → tạo Item đúng loại
     *   3. itemDAO.save(item)
     *   4. Tạo Auction: status=OPENING, endTime=now+durationDays
     *   5. auctionDAO.save(auction)
     *   6. Trả về AuctionSummaryDTO
     */
    public Response createAuction(CreateAuctionDTO dto, String sellerId) {
        // 1. Validate
        if (dto == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin tạo phiên", null);
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Tên sản phẩm không được để trống", null);
        }
        if (dto.getStartingPrice() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Giá khởi điểm phải lớn hơn 0", null);
        }
        if (dto.getItemType() == null || dto.getItemType().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Loại sản phẩm không được để trống", null);
        }
        if (dto.getDurationDays() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thời gian đấu giá phải lớn hơn 0 ngày", null);
        }

        // 2. FACTORY PATTERN — tạo Item đúng loại
        String itemId = UUID.randomUUID().toString();
        String condition = dto.getCondition() != null ? dto.getCondition() : "Mới";
        Item item;

        switch (dto.getItemType().toUpperCase()) {
            case "ELECTRONICS":
                item = ItemFactory.createElectronics(
                        itemId, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getBrand(), dto.getWarrantyMonths());
                break;
            case "ART":
                item = ItemFactory.createArt(
                        itemId, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getArtistName(), dto.getMaterial(), dto.getCreationYear());
                break;
            case "VEHICLE":
                item = ItemFactory.createVehicle(
                        itemId, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getBrand(), dto.getModel(), dto.getYear(), dto.getKm());
                break;
            default:
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Loại sản phẩm không hợp lệ. Chỉ chấp nhận: ELECTRONICS, ART, VEHICLE", null);
        }

        // 3. Lưu Item
        itemDAO.save(item);

        // 4. Tạo Auction
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction(
                UUID.randomUUID().toString(),
                itemId,
                dto.getStartingPrice(),
                now,
                now.plusDays(dto.getDurationDays())
        );
        auction.setStatus(AuctionStatus.OPENING); // Override PENDING → OPENING ngay

        // 5. Lưu Auction
        auctionDAO.save(auction);

        // 6. Trả về thông tin tóm tắt
        AuctionSummaryDTO result = new AuctionSummaryDTO(
                auction.getId(), item.getName(),
                auction.getCurrentPrice(), auction.getStatus().name()
        );
        System.out.println("[AuctionService] CREATE: item=" + item.getName()
                + " | loại=" + dto.getItemType()
                + " | giá khởi=" + String.format("%,.0f", dto.getStartingPrice()) + " VNĐ"
                + " | auctionId=" + auction.getId());
        return new Response(ResponseStatus.SUCCESS, "Tạo phiên đấu giá thành công!", result);
    }

    /**
     * Đóng phiên đấu giá thủ công.
     *
     * Luồng:
     *   1. Tìm phiên + kiểm tra đang OPENING
     *   2. Cập nhật status → CLOSED
     *   3. Thông báo cho tất cả observer (push notification "phiên đã đóng")
     *   4. Trả về kết quả: winner + giá cuối
     */
    public Response closeAuction(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }
        Auction auction = auctionDAO.findById(auctionId.trim());
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy phiên đấu giá", null);
        }
        if (auction.getStatus() != AuctionStatus.OPENING) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Phiên đấu giá không thể đóng — trạng thái hiện tại: " + auction.getStatus(), null);
        }

        // Đóng phiên
        auction.setStatus(AuctionStatus.CLOSED);
        auctionDAO.update(auction);

        // Thông báo realtime cho tất cả client đang xem phiên này
        AuctionManager.getInstance().notifyBidUpdate(
                auctionId, auction.getCurrentPrice(), "SYSTEM_CLOSE");

        // Tìm tên winner
        String winnerName = "Không có người đặt giá";
        if (auction.getCurrentWinnerId() != null) {
            User winner = userDAO.findById(auction.getCurrentWinnerId());
            if (winner != null) winnerName = winner.getFullName();
        }

        String resultMsg = "Phiên đã đóng! Winner: " + winnerName
                + " | Giá cuối: " + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ";
        System.out.println("[AuctionService] CLOSE: auctionId=" + auctionId
                + " | winner=" + winnerName
                + " | giá cuối=" + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ");
        return new Response(ResponseStatus.SUCCESS, resultMsg, auction.getCurrentPrice());
    }
}

