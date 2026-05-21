package com.auction.server.service;

import com.auction.model.dto.AuctionDetailDTO;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.Item;
import com.auction.model.entity.User;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;

import java.time.format.DateTimeFormatter;

/**
 * Lớp chịu trách nhiệm nhặt nhạnh và chuyển đổi dữ liệu từ Entity sang DTO.
 */
public class AuctionMapper {
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private final BidTransactionDAO bidTransactionDAO;

    public AuctionMapper(ItemDAO itemDAO, UserDAO userDAO, BidTransactionDAO bidTransactionDAO) {
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    public AuctionSummaryDTO toSummaryDTO(Auction auction) {
        Item item = itemDAO.findById(auction.getItemId());
        String itemName = (item != null) ? item.getName() : "Không rõ";
        String itemType = null;
        double startingPrice = 0;
        String sellerId = null;
        String sellerName = null;

        if (item != null) {
            startingPrice = item.getStartingPrice();
            sellerId = item.getSellerId();
            itemType = item.getItemType().name();

            if (sellerId != null) {
                User seller = userDAO.findById(sellerId);
                if (seller != null) {
                    sellerName = seller.getFullName() != null ? seller.getFullName() : seller.getUsername();
                }
            }
        }

        int bidCount = bidTransactionDAO.findByAuctionId(auction.getId()).size();

        String endTime = auction.getEndTime() != null
                ? auction.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : null;

        return new AuctionSummaryDTO(
                auction.getId(),
                itemName,
                auction.getCurrentPrice(),
                auction.getStatus().name(),
                startingPrice,
                sellerName,
                sellerId,
                itemType,
                bidCount,
                endTime,
                auction.getCurrentWinnerId());
    }

    public AuctionDetailDTO toDetailDTO(Auction auction) {
        Item item = itemDAO.findById(auction.getItemId());

        String sellerName = "Không rõ";
        if (item != null && item.getSellerId() != null) {
            User seller = userDAO.findById(item.getSellerId());
            if (seller != null)
                sellerName = seller.getFullName();
        }

        String winnerName = "Chưa có";
        if (auction.getCurrentWinnerId() != null) {
            User winner = userDAO.findById(auction.getCurrentWinnerId());
            if (winner != null)
                winnerName = winner.getFullName();
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
        return detail;
    }
}
