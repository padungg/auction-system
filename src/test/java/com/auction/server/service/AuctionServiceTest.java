package com.auction.server.service;

import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.entity.*;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests cho AuctionService.
 * Sử dụng manual stubs cho DAO interfaces để tránh lỗi Mockito Java 25.
 */
@DisplayName("AuctionService Tests")
class AuctionServiceTest {

  static class AuctionDAOStub implements AuctionDAO {
    private final Map<String, Auction> auctions = new HashMap<>();
    public int saveCount = 0;
    public int updateCount = 0;
    public int deleteCount = 0;

    public void addAuction(Auction a) {
      auctions.put(a.getId(), a);
    }

    @Override
    public List<Auction> findAllByStatus(AuctionStatus status) {
      return auctions.values().stream()
          .filter(a -> a.getStatus() == status)
          .collect(Collectors.toList());
    }

    @Override
    public Auction findById(String id) {
      return auctions.get(id);
    }

    @Override
    public boolean save(Auction auction) {
      auctions.put(auction.getId(), auction);
      saveCount++;
      return true;
    }

    @Override
    public boolean update(Auction auction) {
      auctions.put(auction.getId(), auction);
      updateCount++;
      return true;
    }

    @Override
    public boolean delete(String id) {
      boolean rem = auctions.remove(id) != null;
      if (rem)
        deleteCount++;
      return rem;
    }

    @Override
    public List<Auction> findAll() {
      return new ArrayList<>(auctions.values());
    }

    @Override
    public List<Auction> findByCurrentWinnerId(String winnerId) {
      return auctions.values().stream().filter(a -> winnerId.equals(a.getCurrentWinnerId()))
          .collect(Collectors.toList());
    }
  }

  static class ItemDAOStub implements ItemDAO {
    private final Map<String, Item> items = new HashMap<>();
    public int saveCount = 0;
    public int updateCount = 0;

    public void addItem(Item i) {
      items.put(i.getId(), i);
    }

    @Override
    public Item findById(String id) {
      return items.get(id);
    }

    @Override
    public boolean save(Item item) {
      items.put(item.getId(), item);
      saveCount++;
      return true;
    }

    @Override
    public boolean update(Item item) {
      items.put(item.getId(), item);
      updateCount++;
      return true;
    }

    @Override
    public boolean delete(String id) {
      return items.remove(id) != null;
    }
  }

  static class UserDAOStub implements UserDAO {
    private final Map<String, User> users = new HashMap<>();
    public int updateCount = 0;

    public void addUser(User u) {
      users.put(u.getId(), u);
    }

    @Override
    public User findByUsername(String username) {
      return null;
    }

    @Override
    public User findById(String id) {
      return users.get(id);
    }

    @Override
    public boolean existsByUsername(String username) {
      return false;
    }

    @Override
    public boolean save(User user) {
      return true;
    }

    public boolean update(User user) {
      users.put(user.getId(), user);
      updateCount++;
      return true;
    }

    public List<User> findAll() {
      return new ArrayList<>(users.values());
    }

    public boolean delete(String id) {
      return true;
    }

    @Override
    public User findFirstByRole(com.auction.model.entity.UserRole role) {
      return users.values().stream().filter(u -> u.getRole() == role).findFirst().orElse(null);
    }
  }

  static class BidTransactionDAOStub implements com.auction.server.dao.BidTransactionDAO {
    @Override
    public boolean save(com.auction.model.entity.BidTransaction bid) {
      return true;
    }

    @Override
    public List<com.auction.model.entity.BidTransaction> findByAuctionId(String auctionId) {
      return new ArrayList<>();
    }

    @Override
    public List<com.auction.model.entity.BidTransaction> findByBidderId(String bidderId) {
      return new ArrayList<>();
    }
  }

  static class AutoBidDAOStub implements com.auction.server.dao.AutoBidDAO {
    @Override
    public List<com.auction.model.entity.AutoBidEntry> findAll() {
      return new ArrayList<>();
    }

    @Override
    public boolean save(com.auction.model.entity.AutoBidEntry entry) {
      return true;
    }

    @Override
    public boolean delete(String auctionId, String userId) {
      return true;
    }

    @Override
    public boolean deleteByAuctionId(String auctionId) {
      return true;
    }
  }

  private AuctionDAOStub auctionDAO;
  private ItemDAOStub itemDAO;
  private UserDAOStub userDAO;
  private BidTransactionDAOStub bidTransactionDAO;

  private AuctionService auctionService;
  private ItemService itemService;
  private AuctionMapper auctionMapper;
  private AutoBidService autoBidService;

  private User seller;
  private Item sampleItem;
  private Auction pendingAuction;
  private Auction runningAuction;

  @BeforeEach
  void setUp() {
    auctionDAO = new AuctionDAOStub();
    itemDAO = new ItemDAOStub();
    userDAO = new UserDAOStub();
    bidTransactionDAO = new BidTransactionDAOStub();

    itemService = new ItemService(itemDAO);
    auctionMapper = new AuctionMapper(itemDAO, userDAO, bidTransactionDAO);
    autoBidService = new AutoBidService(auctionDAO, new AutoBidDAOStub());
    auctionService = new AuctionService(auctionDAO, userDAO, itemService, auctionMapper, autoBidService);

    seller = new User("seller-001", "seller1", "pass", "s@mail.com",
        "Seller One", "0900000000", "HN", true, UserRole.MEMBER, 0.0, "MyShop", 4.5);
    userDAO.addUser(seller);

    sampleItem = new Electronics("item-001", "iPhone 15", "Điện thoại mới", "Mới",
        "seller-001", 20_000_000.0, "Apple", 12);
    itemDAO.addItem(sampleItem);

    pendingAuction = new Auction("auc-pending", "item-001", 20_000_000.0,
        LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1));
    pendingAuction.setStatus(AuctionStatus.OPEN);
    auctionDAO.addAuction(pendingAuction);

    runningAuction = new Auction("auc-running", "item-001", 20_000_000.0,
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
    runningAuction.setStatus(AuctionStatus.RUNNING);
    auctionDAO.addAuction(runningAuction);
  }

  // CREATE AUCTION
  @Nested
  @DisplayName("createAuction()")
  class CreateAuctionTests {

    private CreateAuctionDTO createDto;

    @BeforeEach
    void setUp() {
      createDto = new CreateAuctionDTO();
      createDto.setItemType(ItemType.ELECTRONICS.name());
      createDto.setName("MacBook");
      createDto.setCondition("Mới");
      createDto.setStartingPrice(30_000_000.0);
      createDto.setBrand("Apple");
      createDto.setWarrantyMonths(24);
      createDto.setDurationDays(3);
      createDto.setDurationHours(72);
      createDto.setStepPrice(50000.0);
    }

    @Test
    @DisplayName("TC-AUC-CREATE-01: DTO null → BAD_REQUEST")
    void create_nullDto() {
      assertEquals(ResponseStatus.BAD_REQUEST, auctionService.createAuction(null, "seller-001").getStatus());
    }

    @Test
    @DisplayName("TC-AUC-CREATE-03: Validate fail (tên rỗng) → ném ValidationException")
    void create_invalidDto() {
      createDto.setName("");
      assertThrows(com.auction.server.util.ValidationException.class,
          () -> auctionService.createAuction(createDto, "seller-001"));
    }

    @Test
    @DisplayName("TC-AUC-CREATE-04: Thời gian không hợp lệ (duration <= 0) → BAD_REQUEST")
    void create_invalidTime() {
      createDto.setDurationHours(0);
      assertEquals(ResponseStatus.BAD_REQUEST, auctionService.createAuction(createDto, "seller-001").getStatus());
    }

    @Test
    @DisplayName("TC-AUC-CREATE-05: Tạo thành công → SUCCESS + lưu Item & Auction")
    void create_success() {
      Response res = auctionService.createAuction(createDto, "seller-001");
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertEquals(1, itemDAO.saveCount);
      assertEquals(1, auctionDAO.saveCount);
    }

    @Test
    @DisplayName("TC-AUC-CREATE-06: Giá khởi điểm bằng 0 → BAD_REQUEST")
    void create_startingPriceZero() {
      createDto.setStartingPrice(0.0);
      assertEquals(ResponseStatus.BAD_REQUEST,
          auctionService.createAuction(createDto, "seller-001").getStatus());
    }

    @Test
    @DisplayName("TC-AUC-CREATE-07: Giá khởi điểm âm → BAD_REQUEST")
    void create_startingPriceNegative() {
      createDto.setStartingPrice(-1_000.0);
      assertEquals(ResponseStatus.BAD_REQUEST,
          auctionService.createAuction(createDto, "seller-001").getStatus());
    }

    @Test
    @DisplayName("TC-AUC-CREATE-08: Loại sản phẩm không hợp lệ → ValidationException")
    void create_unknownItemType() {
      createDto.setItemType("UNKNOWN_TYPE");
      assertThrows(com.auction.server.util.ValidationException.class,
          () -> auctionService.createAuction(createDto, "seller-001"));
    }
  }

  // UPDATE AUCTION
  @Nested
  @DisplayName("updateAuction()")
  class UpdateAuctionTests {
    private UpdateAuctionDTO updateDto;

    @BeforeEach
    void setUp() {
      updateDto = new UpdateAuctionDTO();
      updateDto.setAuctionId("auc-pending");
      updateDto.setName("iPhone 15 Pro");
      updateDto.setCondition("Cũ");
      updateDto.setStartingPrice(18_000_000.0);
    }

    @Test
    @DisplayName("TC-AUC-UPDATE-01: Phiên không tồn tại → ValidationException")
    void update_notFound() {
      updateDto.setAuctionId("ghost");
      assertThrows(com.auction.server.util.ValidationException.class,
          () -> auctionService.updateAuctionItem(updateDto, "seller-001"));
    }

    @Test
    @DisplayName("TC-AUC-UPDATE-02: Không phải người bán → ValidationException")
    void update_notSeller() {
      assertThrows(com.auction.server.util.ValidationException.class,
          () -> auctionService.updateAuctionItem(updateDto, "other-user"));
    }

    @Test
    @DisplayName("TC-AUC-UPDATE-03: Đã có người đặt giá → BAD_REQUEST")
    void update_notPending() {
      runningAuction.setCurrentWinnerId("some-user");
      updateDto.setAuctionId("auc-running");
      assertEquals(ResponseStatus.BAD_REQUEST,
          auctionService.updateAuctionItem(updateDto, "seller-001").getStatus());
    }

    @Test
    @DisplayName("TC-AUC-UPDATE-04: Cập nhật thành công → SUCCESS + thay đổi DB")
    void update_success() {
      Response res = auctionService.updateAuctionItem(updateDto, "seller-001");
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertEquals(1, itemDAO.updateCount);
      assertEquals(1, auctionDAO.updateCount);
    }
  }

  // DELETE AUCTION
  @Nested
  @DisplayName("deleteAuctionItem()")
  class DeleteAuctionTests {

    @Test
    @DisplayName("TC-AUC-DEL-01: Không tồn tại → ValidationException")
    void delete_notFound() {
      assertThrows(com.auction.server.util.ValidationException.class,
          () -> auctionService.deleteAuctionItem("ghost", "seller-001"));
    }

    @Test
    @DisplayName("TC-AUC-DEL-02: Không phải chủ sở hữu → ValidationException")
    void delete_notOwner() {
      assertThrows(com.auction.server.util.ValidationException.class,
          () -> auctionService.deleteAuctionItem("auc-pending", "hacker"));
    }

    @Test
    @DisplayName("TC-AUC-DEL-03: Đã có người đặt giá không thể xóa → BAD_REQUEST")
    void delete_running() {
      runningAuction.setCurrentWinnerId("some-buyer");
      assertEquals(ResponseStatus.BAD_REQUEST,
          auctionService.deleteAuctionItem("auc-running", "seller-001").getStatus());
    }

    @Test
    @DisplayName("TC-AUC-DEL-04: Xóa thành công → SUCCESS")
    void delete_success() {
      assertEquals(ResponseStatus.SUCCESS,
          auctionService.deleteAuctionItem("auc-pending", "seller-001").getStatus());
      assertEquals(1, auctionDAO.deleteCount);
    }
  }

  // GET ACTIVE AUCTIONS
  @Nested
  @DisplayName("getAllAuctions()")
  class GetActiveAuctionsTests {

    @Test
    @DisplayName("TC-AUC-GET-01: Trả về danh sách DTO của tất cả phiên đấu giá")
    void getActive_success() {
      Response res = auctionService.getAllAuctions();
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertInstanceOf(List.class, res.getPayload());

      @SuppressWarnings("unchecked")
      List<AuctionSummaryDTO> list = (List<AuctionSummaryDTO>) res.getPayload();
      // getAllAuctions() trả về TẤT CẢ phiên (không lọc theo status),
      // setup có 2 phiên: auc-pending (OPEN) và auc-running (RUNNING)
      assertEquals(2, list.size());
      assertTrue(list.stream().anyMatch(dto -> "auc-running".equals(dto.getAuctionId())));
    }
  }

  // CLOSE AUCTION
  @Nested
  @DisplayName("closeAuction()")
  class CloseAuctionTests {

    @Test
    @DisplayName("TC-AUC-CLOSE-01: Đóng phiên không có người mua → FINISHED")
    void close_noWinner() {
      auctionService.closeAuction("auc-running");

      assertEquals(AuctionStatus.FINISHED, runningAuction.getStatus());
      assertEquals(1, auctionDAO.updateCount);
    }

    @Test
    @DisplayName("TC-AUC-CLOSE-02: Đóng phiên CÓ người mua → FINISHED, trả về tên winner trong message")
    void close_withWinner() {
      User buyer = new User("buyer-001", "buyer1", "pass", "b@mail.com",
          "Buyer", "090", "HCM", true, UserRole.MEMBER, 30_000_000.0, null, 0.0);
      userDAO.addUser(buyer);

      runningAuction.setCurrentWinnerId("buyer-001");
      runningAuction.setCurrentPrice(20_000_000.0);

      Response res = auctionService.closeAuction("auc-running");

      assertEquals(AuctionStatus.FINISHED, runningAuction.getStatus());
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertTrue(res.getMessage().contains("Buyer"));
    }

    @Test
    @DisplayName("TC-AUC-CLOSE-03: Đóng phiên đã FINISHED → BAD_REQUEST")
    void close_alreadyFinished() {
      runningAuction.setStatus(AuctionStatus.FINISHED);
      Response res = auctionService.closeAuction("auc-running");
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }
  }

  @Nested
  @DisplayName("adminCancelAuction() và adminMarkPaid()")
  class AdminOperationTests {

    @Test
    @DisplayName("TC-AUC-ADMIN-01: cancelAuction phiên không tồn tại → ValidationException")
    void adminCancel_notFound() {
      assertThrows(com.auction.server.util.ValidationException.class,
          () -> auctionService.adminCancelAuction("ghost-id"));
    }

    @Test
    @DisplayName("TC-AUC-ADMIN-02: cancelAuction thành công → SUCCESS + status CANCELED")
    void adminCancel_success() {
      Response res = auctionService.adminCancelAuction("auc-running");
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertEquals(AuctionStatus.CANCELED, runningAuction.getStatus());
    }

    @Test
    @DisplayName("TC-AUC-ADMIN-03: markPaid phiên chưa FINISHED → BAD_REQUEST")
    void adminMarkPaid_notFinished() {
      assertEquals(ResponseStatus.BAD_REQUEST,
          auctionService.adminMarkPaid("auc-running").getStatus());
    }

    @Test
    @DisplayName("TC-AUC-ADMIN-04: markPaid thành công → SUCCESS + status PAID")
    void adminMarkPaid_success() {
      runningAuction.setStatus(AuctionStatus.FINISHED);
      Response res = auctionService.adminMarkPaid("auc-running");
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertEquals(AuctionStatus.PAID, runningAuction.getStatus());
    }
  }
}
