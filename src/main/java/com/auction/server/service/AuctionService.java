package com.auction.server.service;

import com.auction.model.dto.AuctionDetailDTO;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.Item;
import com.auction.model.entity.User;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.observer.AuctionManager;
import com.auction.server.util.ValidationException;
import com.auction.server.util.ValidationUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý nghiệp vụ liên quan đến Phiên Đấu Giá.
 */
public class AuctionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionService.class);
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final ItemService itemService;
    private final AuctionMapper auctionMapper;
    private final AutoBidService autoBidService;

    public AuctionService(AuctionDAO auctionDAO, UserDAO userDAO, ItemService itemService, AuctionMapper auctionMapper,
            AutoBidService autoBidService) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
        this.itemService = itemService;
        this.auctionMapper = auctionMapper;
        this.autoBidService = autoBidService;
    }

    public Response getAllAuctions() {
        List<Auction> auctions = auctionDAO.findAll();
        List<AuctionSummaryDTO> summaryList = new ArrayList<>();
        for (Auction auction : auctions) {
            summaryList.add(auctionMapper.toSummaryDTO(auction));
        }
        LOGGER.info("GET_ALL: {} phiên đang mở", summaryList.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy danh sách thành công", summaryList);
    }

    public Response getAuctionDetail(String auctionId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        AuctionDetailDTO detail = auctionMapper.toDetailDTO(auction);
        return new Response(ResponseStatus.SUCCESS, "Lấy chi tiết phiên thành công", detail);
    }

    public Response createAuction(CreateAuctionDTO dto, String sellerId) throws ValidationException {
        if (dto == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin tạo phiên", null);
        }
        ValidationUtils.requireNonBlank(dto.getName(), "Tên sản phẩm");
        ValidationUtils.requireNonBlank(dto.getItemType(), "Loại sản phẩm");
        if (dto.getStartingPrice() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Giá khởi điểm phải lớn hơn 0", null);
        }
        if (dto.getStepPrice() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Bước giá tối thiểu phải lớn hơn 0", null);
        }
        if (dto.getDurationHours() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thời gian đấu giá phải lớn hơn 0 giờ", null);
        }

        LocalDateTime startTime = LocalDateTime.now();
        if (dto.getStartTimeStr() != null && !dto.getStartTimeStr().trim().isEmpty()) {
            String startStr = dto.getStartTimeStr().trim();
            try {
                if (startStr.length() == 16) { // yyyy-MM-dd HH:mm
                    startTime = LocalDateTime.parse(startStr,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } else if (startStr.length() == 19) { // yyyy-MM-dd HH:mm:ss
                    startTime = LocalDateTime.parse(startStr,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else {
                    return new Response(ResponseStatus.BAD_REQUEST,
                            "Thời gian bắt đầu không đúng định dạng yyyy-MM-dd HH:mm", null);
                }
            } catch (Exception e) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Thời gian bắt đầu không đúng định dạng yyyy-MM-dd HH:mm", null);
            }
        }

        LocalDateTime endTime = startTime.plusHours(dto.getDurationHours());
        if (!endTime.isAfter(startTime)) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu", null);
        }

        // Tạo Item thông qua ItemService
        Item item = itemService.createItem(dto, sellerId);

        // Tạo Auction
        Auction auction = new Auction(
                UUID.randomUUID().toString(),
                item.getId(),
                dto.getStartingPrice(),
                startTime,
                endTime);
        auction.setStepPrice(dto.getStepPrice());

        if (startTime.isAfter(LocalDateTime.now())) {
            auction.setStatus(AuctionStatus.OPEN);
        } else {
            auction.setStatus(AuctionStatus.RUNNING);
        }

        auctionDAO.save(auction);

        AuctionSummaryDTO result = auctionMapper.toSummaryDTO(auction);
        LOGGER.info("CREATE: item={} | loại={} | giá khởi={} VNĐ | auctionId={}",
                item.getName(), dto.getItemType(), String.format("%,.0f", dto.getStartingPrice()), auction.getId());
        return new Response(ResponseStatus.SUCCESS, "Tạo phiên đấu giá thành công!", result);
    }

    public Response closeAuction(String auctionId) throws ValidationException {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException("Thiếu mã phiên đấu giá");
        }

        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            Auction auction = auctionDAO.findById(auctionId.trim());
            if (auction == null) {
                throw new ValidationException("Không tìm thấy phiên đấu giá");
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Phiên đấu giá không thể đóng — trạng thái hiện tại: " + auction.getStatus(), null);
            }

            auction.setStatus(AuctionStatus.FINISHED);
            auctionDAO.update(auction);

            // Dọn dẹp AutoBid queue để tránh Memory Leak
            autoBidService.clearAuction(auctionId);

            AuctionManager.getInstance().notifyAuctionClosed(
                    auctionId, auction.getCurrentPrice(), auction.getCurrentWinnerId());

            String winnerName = "Không có người đặt giá";
            if (auction.getCurrentWinnerId() != null) {
                User winner = userDAO.findById(auction.getCurrentWinnerId());
                if (winner != null)
                    winnerName = winner.getFullName();
            }

            String resultMsg = "Phiên đã đóng! Winner: " + winnerName
                    + " | Giá cuối: " + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ";
            LOGGER.info("CLOSE: auctionId={} | winner={} | giá cuối={} VNĐ",
                    auctionId, winnerName, String.format("%,.0f", auction.getCurrentPrice()));

            com.auction.server.util.LockManager.removeAuctionLock(auctionId);

            return new Response(ResponseStatus.SUCCESS, resultMsg, auction.getCurrentPrice());
        }
    }

    public Response updateAuctionItem(UpdateAuctionDTO dto, String sellerId) throws ValidationException {
        if (dto == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin", null);
        }
        Object lock = com.auction.server.util.LockManager.getAuctionLock(dto.getAuctionId());
        synchronized (lock) {
            Auction auction = validateAndGetAuction(dto.getAuctionId());
            if (auction.getCurrentWinnerId() != null) {
                return new Response(ResponseStatus.BAD_REQUEST, "Không thể sửa sản phẩm đã có người đặt giá", null);
            }
            if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
                return new Response(ResponseStatus.BAD_REQUEST, "Không thể sửa phiên đấu giá đã kết thúc", null);
            }

            itemService.updateItem(auction.getItemId(), dto, sellerId);

            if (dto.getStartingPrice() > 0) {
                auction.setCurrentPrice(dto.getStartingPrice());
            }
            auctionDAO.update(auction);

            LOGGER.info("UPDATE: auctionId={} by seller={}", auction.getId(), sellerId);
            return new Response(ResponseStatus.SUCCESS, "Cập nhật sản phẩm thành công!", null);
        }
    }

    public Response deleteAuctionItem(String auctionId, String sellerId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        if (auction.getCurrentWinnerId() != null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Không thể xóa sản phẩm đã có người đặt giá", null);
        }

        itemService.deleteItem(auction.getItemId(), sellerId);
        auctionDAO.delete(auction.getId());

        // Tránh memory leak
        com.auction.server.util.LockManager.removeAuctionLock(auctionId);

        LOGGER.info("DELETE: auctionId={} by seller={}", auction.getId(), sellerId);
        return new Response(ResponseStatus.SUCCESS, "Xóa sản phẩm thành công!", null);
    }

    // ADMIN OPERATIONS

    public Response adminCancelAuction(String auctionId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        auction.setStatus(AuctionStatus.CANCELED);
        auctionDAO.update(auction);
        LOGGER.info("ADMIN_CANCEL_AUCTION: auctionId={}", auctionId);
        return new Response(ResponseStatus.SUCCESS, "Đã hủy phiên đấu giá thành công!", null);
    }

    public Response adminMarkPaid(String auctionId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        if (auction.getStatus() != AuctionStatus.FINISHED) {
            return new Response(ResponseStatus.BAD_REQUEST, "Chỉ có thể đánh dấu PAID cho phiên đã kết thúc", null);
        }
        auction.setStatus(AuctionStatus.PAID);
        auctionDAO.update(auction);
        LOGGER.info("ADMIN_MARK_PAID: auctionId={}", auctionId);
        return new Response(ResponseStatus.SUCCESS, "Đã đánh dấu phiên thành PAID thành công!", null);
    }

    private Auction validateAndGetAuction(String auctionId) throws ValidationException {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException("Thiếu mã phiên đấu giá");
        }
        Auction auction = auctionDAO.findById(auctionId.trim());
        if (auction == null) {
            throw new ValidationException("Không tìm thấy phiên đấu giá");
        }
        return auction;
    }
}
