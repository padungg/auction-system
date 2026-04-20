package com.auction.server.service;

import com.auction.model.dto.AuctionDetailDTO;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.entity.*;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ liên quan đến Phiên Đấu Giá.
 *
 * Chức năng:
 *   - Lấy danh sách các phiên đang mở (trang chủ)
 *   - Xem chi tiết 1 phiên đấu giá
 *   - Tạo phiên đấu giá mới (đăng bán sản phẩm)
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
            // Lấy tên item cho mỗi phiên
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

        System.out.println(">>> [AuctionService] Trả về " + summaryList.size() + " phiên đấu giá");
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

        // Tìm phiên đấu giá
        Auction auction = auctionDAO.findById(auctionId.trim());
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy phiên đấu giá", null);
        }

        // Lấy thông tin item
        Item item = itemDAO.findById(auction.getItemId());

        // Lấy tên seller
        String sellerName = "Không rõ";
        if (item != null && item.getSellerId() != null) {
            User seller = userDAO.findById(item.getSellerId());
            if (seller != null) sellerName = seller.getFullName();
        }

        // Lấy tên winner hiện tại
        String winnerName = "Chưa có";
        if (auction.getCurrentWinnerId() != null) {
            User winner = userDAO.findById(auction.getCurrentWinnerId());
            if (winner != null) winnerName = winner.getFullName();
        }

        // Tạo DTO chi tiết
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

        return new Response(ResponseStatus.SUCCESS, "Lấy chi tiết thành công", detail);
    }

    /**
     * Tạo phiên đấu giá mới (đăng bán sản phẩm).
     *
     * Luồng:
     *   1. Dùng ItemFactory tạo Item đúng loại (Vehicle/Electronics/Art)
     *   2. Lưu Item vào database
     *   3. Tạo Auction với trạng thái OPENING
     *   4. Lưu Auction vào database
     */
    public Response createAuction(CreateAuctionDTO dto, String sellerId) {
        // Validation
        if (dto == null || dto.getItemType() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin sản phẩm", null);
        }

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Tên sản phẩm không được để trống", null);
        }

        if (dto.getStartingPrice() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Giá khởi điểm phải lớn hơn 0", null);
        }

        if (dto.getDurationDays() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thời gian đấu giá phải lớn hơn 0 ngày", null);
        }

        // Tạo Item dùng Factory Pattern
        String itemId = UUID.randomUUID().toString();
        Item item;

        try {
            switch (dto.getItemType().toUpperCase()) {
                case "VEHICLE":
                    item = ItemFactory.createVehicle(
                            itemId, dto.getName(), dto.getDescription(), dto.getCondition(),
                            sellerId, dto.getStartingPrice(),
                            dto.getBrand(), dto.getModel(), dto.getYear(), dto.getKm()
                    );
                    break;
                case "ELECTRONICS":
                    item = ItemFactory.createElectronics(
                            itemId, dto.getName(), dto.getDescription(), dto.getCondition(),
                            sellerId, dto.getStartingPrice(),
                            dto.getBrand(), dto.getWarrantyMonths()
                    );
                    break;
                case "ART":
                    item = ItemFactory.createArt(
                            itemId, dto.getName(), dto.getDescription(), dto.getCondition(),
                            sellerId, dto.getStartingPrice(),
                            dto.getArtistName(), dto.getMaterial(), dto.getCreationYear()
                    );
                    break;
                default:
                    return new Response(ResponseStatus.BAD_REQUEST,
                            "Loại sản phẩm không hợp lệ: " + dto.getItemType()
                                    + ". Chỉ hỗ trợ: VEHICLE, ELECTRONICS, ART", null);
            }
        } catch (Exception e) {
            return new Response(ResponseStatus.ERROR, "Lỗi khi tạo sản phẩm: " + e.getMessage(), null);
        }

        // Lưu Item
        boolean itemSaved = itemDAO.save(item);
        if (!itemSaved) {
            return new Response(ResponseStatus.ERROR, "Lỗi khi lưu sản phẩm", null);
        }

        // Tạo Auction
        String auctionId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(dto.getDurationDays());

        Auction auction = new Auction(auctionId, itemId, dto.getStartingPrice(), startTime, endTime);
        auction.setStatus(AuctionStatus.OPENING); // Mở ngay lập tức

        // Lưu Auction
        boolean auctionSaved = auctionDAO.save(auction);
        if (!auctionSaved) {
            return new Response(ResponseStatus.ERROR, "Lỗi khi tạo phiên đấu giá", null);
        }

        System.out.println(">>> [AuctionService] Phiên đấu giá mới: " + dto.getName()
                + " | Giá khởi điểm: " + dto.getStartingPrice()
                + " | Kết thúc: " + endTime);

        return new Response(ResponseStatus.SUCCESS,
                "Tạo phiên đấu giá thành công! Kết thúc sau " + dto.getDurationDays() + " ngày.", null);
    }

    /**
     * Đóng phiên đấu giá thủ công (Admin hoặc Seller).
     * Xác định người thắng cuộc dựa trên currentWinnerId.
     */
    public Response closeAuction(String auctionId, String userId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }

        Auction auction = auctionDAO.findById(auctionId.trim());
        if (auction == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
        }

        if (auction.getStatus() != AuctionStatus.OPENING) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Phiên này đã đóng hoặc chưa mở (trạng thái: " + auction.getStatus() + ")", null);
        }

        // Đóng phiên
        auction.setStatus(AuctionStatus.CLOSED);
        auctionDAO.update(auction);

        // Xác định người thắng
        String winnerInfo = "Không có ai đặt giá";
        if (auction.getCurrentWinnerId() != null) {
            User winner = userDAO.findById(auction.getCurrentWinnerId());
            if (winner != null) {
                winnerInfo = "Người thắng: " + winner.getFullName()
                        + " | Giá: " + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ";
            }
        }

        System.out.println(">>> [AuctionService] Đã đóng phiên " + auctionId + " | " + winnerInfo);
        return new Response(ResponseStatus.SUCCESS, "Đã đóng phiên đấu giá! " + winnerInfo, null);
    }
}
