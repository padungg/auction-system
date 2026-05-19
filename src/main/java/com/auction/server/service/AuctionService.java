package com.auction.server.service;

import com.auction.model.dto.AuctionDetailDTO;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.Item;
import com.auction.model.entity.ItemFactory;
import com.auction.model.entity.User;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.observer.AuctionManager;
import com.auction.server.util.ValidationUtils;
import com.auction.server.util.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý nghiệp vụ liên quan đến Phiên Đấu Giá.
 * Chức năng:
 * - getAllAuctions() : Lấy danh sách TOÀN BỘ phiên đấu giá (Dùng chung cho cả User và Admin)
 * - getAuctionDetail() : Xem chi tiết 1 phiên
 * - createAuction() : Tạo phiên mới
 * - closeAuction() : Đóng phiên thủ công
 * - Payment: payAuction , getPendingPayments, getPaymentHistory
 */
public class AuctionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionService.class);
    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private final BidTransactionDAO bidTransactionDAO;

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO, UserDAO userDAO,
                          BidTransactionDAO bidTransactionDAO) {
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    /**
     * Lấy danh sách TẤT CẢ các phiên đấu giá (không lọc trạng thái).
     * Phục vụ cho cả màn hình Trang chủ và màn hình Quản lý của Admin.
     * Trả về dạng ĐẦY ĐỦ (AuctionSummaryDTO) với itemType, startingPrice,
     * sellerName, bidCount, endTime.
     */
    public Response getAllAuctions() {
        List<Auction> auctions = auctionDAO.findAll();
        List<AuctionSummaryDTO> summaryList = new ArrayList<>();
        for (Auction auction : auctions) {
            summaryList.add(toSummaryDTO(auction));
        }
        LOGGER.info("GET_ALL: {} phiên đang mở", summaryList.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy danh sách thành công", summaryList);
    }

    /**
     * Lấy chi tiết đầy đủ của 1 phiên đấu giá.
     * Bao gồm: thông tin item, tên seller, tên winner, thời gian, giá...
     */
    public Response getAuctionDetail(String auctionId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
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
        return new Response(ResponseStatus.SUCCESS, "Lấy chi tiết phiên thành công", detail);
    }

    /**
     * Tạo phiên đấu giá mới.
     *
     * FACTORY PATTERN: Dùng ItemFactory để tạo đúng loại Item
     * theo itemType (ELECTRONICS / ART / VEHICLE).
     */
    public Response createAuction(CreateAuctionDTO dto, String sellerId) {
        // 1. Validate
        if (dto == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin tạo phiên", null);
        }
        ValidationUtils.requireNonBlank(dto.getName(), "Tên sản phẩm");
        ValidationUtils.requireNonBlank(dto.getItemType(), "Loại sản phẩm");
        if (dto.getStartingPrice() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Giá khởi điểm phải lớn hơn 0", null);
        }
        if (dto.getDurationDays() <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thời gian đấu giá phải lớn hơn 0 ngày", null);
        }

        // 2. FACTORY PATTERN — tạo Item đúng loại
        String itemId = UUID.randomUUID().toString();
        String condition = dto.getCondition() != null ? dto.getCondition() : "Mới";
        Item item;

        switch (dto.getItemType().toUpperCase()) {
            case ItemFactory.TYPE_ELECTRONICS:
                item = ItemFactory.createElectronics(
                        itemId, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getBrand(), dto.getWarrantyMonths());
                break;
            case ItemFactory.TYPE_ART:
                item = ItemFactory.createArt(
                        itemId, dto.getName(), dto.getDescription(), condition, sellerId,
                        dto.getStartingPrice(), dto.getArtistName(), dto.getMaterial(), dto.getCreationYear());
                break;
            case ItemFactory.TYPE_VEHICLE:
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
                now.plusDays(dto.getDurationDays()));
        auction.setStatus(AuctionStatus.RUNNING); // Override OPEN → RUNNING ngay

        // 5. Lưu Auction
        auctionDAO.save(auction);

        // 6. Trả về thông tin tóm tắt
        AuctionSummaryDTO result = toSummaryDTO(auction);
        LOGGER.info("CREATE: item={} | loại={} | giá khởi={} VNĐ | auctionId={}",
                item.getName(),
                dto.getItemType(),
                String.format("%,.0f", dto.getStartingPrice()),
                auction.getId());
        return new Response(ResponseStatus.SUCCESS, "Tạo phiên đấu giá thành công!", result);
    }

    /**
     * Đóng phiên đấu giá thủ công.
     */
    public Response closeAuction(String auctionId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Phiên đấu giá không thể đóng — trạng thái hiện tại: " + auction.getStatus(), null);
        }

        // Đóng phiên
        auction.setStatus(AuctionStatus.FINISHED);
        auctionDAO.update(auction);

        // Thông báo realtime cho tất cả client đang xem phiên này
        AuctionManager.getInstance().notifyAuctionClosed(
                auctionId, auction.getCurrentPrice(), auction.getCurrentWinnerId());

        // Tìm tên winner
        String winnerName = "Không có người đặt giá";
        if (auction.getCurrentWinnerId() != null) {
            User winner = userDAO.findById(auction.getCurrentWinnerId());
            if (winner != null)
                winnerName = winner.getFullName();
        }

        String resultMsg = "Phiên đã đóng! Winner: " + winnerName
                + " | Giá cuối: " + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ";
        LOGGER.info("CLOSE: auctionId={} | winner={} | giá cuối={} VNĐ",
                auctionId,
                winnerName,
                String.format("%,.0f", auction.getCurrentPrice()));
        return new Response(ResponseStatus.SUCCESS, resultMsg, auction.getCurrentPrice());
    }

    /**
     * Cập nhật thông tin sản phẩm của phiên đấu giá.
     * Chỉ cho phép sửa khi chưa có ai đặt giá.
     */
    public Response updateAuctionItem(UpdateAuctionDTO dto, String sellerId) throws ValidationException {
        if (dto == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin", null);
        }
        Auction auction = validateAndGetAuction(dto.getAuctionId());
        Item item = itemDAO.findById(auction.getItemId());
        if (item == null || !item.getSellerId().equals(sellerId)) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn không có quyền sửa sản phẩm này", null);
        }
        if (auction.getCurrentWinnerId() != null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Không thể sửa sản phẩm đã có người đặt giá", null);
        }
        if (auction.getStatus() == com.auction.model.entity.AuctionStatus.FINISHED ||
                auction.getStatus() == com.auction.model.entity.AuctionStatus.PAID) {
            return new Response(ResponseStatus.BAD_REQUEST, "Không thể sửa phiên đấu giá đã kết thúc", null);
        }

        // Cập nhật Item
        if (dto.getName() != null && !dto.getName().trim().isEmpty())
            item.setName(dto.getName());
        if (dto.getDescription() != null)
            item.setDescription(dto.getDescription());
        if (dto.getCondition() != null)
            item.setCondition(dto.getCondition());
        if (dto.getStartingPrice() > 0) {
            item.setStartingPrice(dto.getStartingPrice());
            auction.setCurrentPrice(dto.getStartingPrice());
        }

        // Cập nhật các trường đặc thù theo loại — dùng đa hình, không cần instanceof
        item.applyUpdate(dto);

        itemDAO.update(item);
        auctionDAO.update(auction);

        LOGGER.info("UPDATE: auctionId={} by seller={}", auction.getId(), sellerId);
        return new Response(ResponseStatus.SUCCESS, "Cập nhật sản phẩm thành công!", null);
    }

    /**
     * Xóa sản phẩm và phiên đấu giá tương ứng.
     * Chỉ cho phép xóa khi chưa có ai đặt giá.
     */
    public Response deleteAuctionItem(String auctionId, String sellerId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        Item item = itemDAO.findById(auction.getItemId());
        if (item == null || !item.getSellerId().equals(sellerId)) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn không có quyền xóa sản phẩm này", null);
        }
        if (auction.getCurrentWinnerId() != null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Không thể xóa sản phẩm đã có người đặt giá", null);
        }

        auctionDAO.delete(auction.getId());
        itemDAO.delete(item.getId());

        LOGGER.info("DELETE: auctionId={} by seller={}", auction.getId(), sellerId);
        return new Response(ResponseStatus.SUCCESS, "Xóa sản phẩm thành công!", null);
    }

    // PAYMENT OPERATIONS

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
                pending.add(toSummaryDTO(a));
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
                history.add(toSummaryDTO(a));
            }
        }
        LOGGER.info("GET_PAYMENT_HISTORY: user={} | {} phiên", userId, history.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy lịch sử thanh toán thành công", history);
    }

    /**
     * Thanh toán phiên đấu giá đã thắng.
     * - Buyer trả: giá trúng + 2% phí dịch vụ
     * - Seller nhận: giá trúng (không có phí)
     * - Admin nhận: 2% phí dịch vụ
     */
    public Response payAuction(String auctionId, String userId) throws ValidationException {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        Auction auction = validateAndGetAuction(auctionId);
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
        double platformFee = Math.round(basePrice * 0.02 * 100.0) / 100.0; // 2% phí
        double totalRequired = basePrice + platformFee;

        if (user.getBalance() < totalRequired) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Số dư không đủ! Cần: " + String.format("%,.0f", totalRequired)
                            + " VNĐ (gồm 2%% phí) | Có: "
                            + String.format("%,.0f", user.getBalance()) + " VNĐ",
                    null);
        }

        // Thực hiện giao dịch tài chính
        processPaymentTransaction(user, basePrice, platformFee, totalRequired, auction.getItemId());

        // 4. Chuyển status → PAID
        auction.setStatus(AuctionStatus.PAID);
        auctionDAO.update(auction);

        LOGGER.info("PAY_AUCTION: auctionId={} | buyer={} | giá={} | phí={} | tổng={} VNĐ",
                auctionId,
                userId,
                String.format("%,.0f", basePrice),
                String.format("%,.0f", platformFee),
                String.format("%,.0f", totalRequired));
        return new Response(ResponseStatus.SUCCESS,
                "Thanh toán thành công! (Đã trợ phí 2%% = "
                        + String.format("%,.0f", platformFee) + " VNĐ)",
                null);
    }

    /**
     * Xử lý giao dịch
     */
    private void processPaymentTransaction(User buyer, double basePrice, double platformFee, double totalRequired,
                                           String itemId) {
        // 1. Trừ tiền buyer
        buyer.withdraw(totalRequired);
        userDAO.update(buyer);

        // 2. Cộng tiền cho seller
        Item item = itemDAO.findById(itemId);
        if (item != null && item.getSellerId() != null) {
            User seller = userDAO.findById(item.getSellerId());
            if (seller != null) {
                seller.deposit(basePrice);
                userDAO.update(seller);
                LOGGER.info("PAY: seller={} +{} VNĐ", seller.getUsername(), String.format("%,.0f", basePrice));
            }
        }

        // 3. Cộng phí cho Admin
        User admin = userDAO.findFirstByRole(com.auction.model.entity.UserRole.ADMIN);
        if (admin != null) {
            admin.deposit(platformFee);
            userDAO.update(admin);
            LOGGER.info("PAY: admin={} +{} VNĐ (phí 2%%)",
                    admin.getUsername(),
                    String.format("%,.0f", platformFee));
        } else {
            LOGGER.warn("Không tìm thấy Admin để nhận phí!");
        }
    }

    // HELPER — tạo AuctionSummaryDTO đầy đủ

    /**
     * Chuyển Auction entity → AuctionSummaryDTO đầy đủ thông tin.
     * Bao gồm: itemName, itemType, startingPrice, sellerName, sellerId, bidCount,
     * endTime.
     */
    private AuctionSummaryDTO toSummaryDTO(Auction auction) {
        Item item = itemDAO.findById(auction.getItemId());
        String itemName = (item != null) ? item.getName() : "Không rõ";
        String itemType = null;
        double startingPrice = 0;
        String sellerId = null;
        String sellerName = null;

        if (item != null) {
            startingPrice = item.getStartingPrice();
            sellerId = item.getSellerId();

            // Xác định itemType qua đa hình — không cần instanceof
            itemType = item.getItemType();

            // Lấy tên seller
            if (sellerId != null) {
                User seller = userDAO.findById(sellerId);
                if (seller != null) {
                    sellerName = seller.getFullName() != null ? seller.getFullName() : seller.getUsername();
                }
            }
        }

        // Đếm số lượt bid
        int bidCount = bidTransactionDAO.findByAuctionId(auction.getId()).size();

        // Format endTime
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

    // ADMIN OPERATIONS

    public Response adminCancelAuction(String auctionId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        auction.setStatus(com.auction.model.entity.AuctionStatus.CANCELED);
        auctionDAO.update(auction);
        LOGGER.info("ADMIN_CANCEL_AUCTION: auctionId={}", auctionId);
        return new Response(ResponseStatus.SUCCESS, "Đã hủy phiên đấu giá thành công!", null);
    }

    public Response adminMarkPaid(String auctionId) throws ValidationException {
        Auction auction = validateAndGetAuction(auctionId);
        if (auction.getStatus() != com.auction.model.entity.AuctionStatus.FINISHED) {
            return new Response(ResponseStatus.BAD_REQUEST, "Chỉ có thể đánh dấu PAID cho phiên đã kết thúc", null);
        }
        auction.setStatus(com.auction.model.entity.AuctionStatus.PAID);
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
