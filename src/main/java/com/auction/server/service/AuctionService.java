package com.auction.server.service;

import com.auction.model.dto.AuctionDetailDTO;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý nghiệp vụ liên quan đến Phiên Đấu Giá.
 * Chức năng:
 *   - Lấy danh sách các phiên đang mở (trang chủ)
 *   - Xem chi tiết 1 phiên đấu giá
 *   - Tạo phiên đấu giá mới (đăng bán sản phẩm) (chưa làm)
 *   - Đóng phiên đấu giá thủ công (chưa làm)
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
        List<AuctionSummaryDTO> summaryList =  new ArrayList<>();
        for (Auction auction : auctions) {
            Item item = itemDAO.findById(auction.getItemId());
            String itemName = (item != null) ?  item.getName() : "Không rõ";
            AuctionSummaryDTO summary =  new AuctionSummaryDTO(
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
    public Response getAuctionDetail (String auctionId){
        // Check xem auctionId tồn tại không
        if (auctionId == null || auctionId.trim().isEmpty()){
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu mã phiên đấu giá", null);
        }
        // Tìm phiên đấu giá
        Auction auction =  auctionDAO.findById(auctionId.trim());
        if (auction == null){
            return new Response(ResponseStatus.BAD_REQUEST, "Không tìm thấy mã phiên đấu giá", null);
        }
        // Lấy thông tin item
        Item item = itemDAO.findById(auction.getItemId());
        // Lấy tên seller
        String sellerName = "Không rõ";
        if (item != null && item.getSellerId() != null){
            User seller = userDAO.findById(item.getSellerId());
            if (seller != null){
                sellerName = seller.getFullName();
            }
        }
        // Lấy tên winner hiện tại
        String winnerName = "Chưa có";
        if (auction.getCurrentWinnerId() != null){
            User winner = userDAO.findById(auction.getCurrentWinnerId());
            if (winner != null){
                winnerName = winner.getFullName();
            }
        }
        // Tạo DTO chi tiết
        AuctionDetailDTO detail = new  AuctionDetailDTO();
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
        return new Response(ResponseStatus.SUCCESS, "Lấy chi tiết phiên đấu giá thành công", detail);
    }

}
