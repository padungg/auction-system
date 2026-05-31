package com.auction.server.service;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.BidTransaction;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.model.entity.AutoBidEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests cho AutoBidService. */
@DisplayName("AutoBidService Tests")
class AutoBidServiceTest {

  static class AuctionDAOStub implements AuctionDAO {
    private final Map<String, Auction> auctions = new HashMap<>();
    public int updateCount = 0;

    public void addAuction(Auction a) {
      auctions.put(a.getId(), a);
    }

    @Override
    public List<Auction> findAllByStatus(AuctionStatus status) {
      return new ArrayList<>(auctions.values());
    }

    @Override
    public Auction findById(String id) {
      return auctions.get(id);
    }

    @Override
    public boolean save(Auction auction) {
      auctions.put(auction.getId(), auction);
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
      return auctions.remove(id) != null;
    }

    @Override
    public List<Auction> findAll() {
      return new ArrayList<>(auctions.values());
    }

    @Override
    public List<Auction> findByCurrentWinnerId(String winnerId) {
      return auctions.values().stream().filter(a -> winnerId.equals(a.getCurrentWinnerId()))
          .collect(java.util.stream.Collectors.toList());
    }
  }

  static class BidTransactionDAOStub implements BidTransactionDAO {
    public int saveCount = 0;

    @Override
    public boolean save(BidTransaction bid) {
      saveCount++;
      return true;
    }

    @Override
    public List<BidTransaction> findByAuctionId(String auctionId) {
      return new ArrayList<>();
    }

    @Override
    public List<BidTransaction> findByBidderId(String bidderId) {
      return new ArrayList<>();
    }
  }

  static class AutoBidDAOStub implements AutoBidDAO {
    public int saveCount = 0;
    public int deleteCount = 0;

    @Override
    public List<AutoBidEntry> findAll() {
      return new ArrayList<>();
    }

    @Override
    public boolean save(AutoBidEntry entry) {
      saveCount++;
      return true;
    }

    @Override
    public boolean delete(String auctionId, String userId) {
      deleteCount++;
      return true;
    }

    @Override
    public boolean deleteByAuctionId(String auctionId) {
      return true;
    }
  }

  static class ItemDAOStub implements com.auction.server.dao.ItemDAO {
    @Override public com.auction.model.entity.Item findById(String id) { return null; }
    @Override public boolean save(com.auction.model.entity.Item item) { return true; }
    @Override public boolean update(com.auction.model.entity.Item item) { return true; }
    @Override public boolean delete(String id) { return true; }
  }

  private AuctionDAOStub auctionDAO;
  private BidTransactionDAOStub bidTransactionDAO;
  private AutoBidDAOStub autoBidDAO;
  private ItemDAOStub itemDAO;

  private AutoBidService autoBidService;
  private BidService bidService;

  private Auction runningAuction;

  @BeforeEach
  void setUp() {
    auctionDAO = new AuctionDAOStub();
    bidTransactionDAO = new BidTransactionDAOStub();
    autoBidDAO = new AutoBidDAOStub();
    itemDAO = new ItemDAOStub();

    autoBidService = new AutoBidService(auctionDAO, autoBidDAO, itemDAO);
    bidService = new BidService(auctionDAO, bidTransactionDAO, autoBidService, itemDAO, null);
    autoBidService.setBidService(bidService);

    runningAuction = new Auction("auc-001", "item-001", 1_000_000.0,
        LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
    runningAuction.setStatus(AuctionStatus.RUNNING);
    auctionDAO.addAuction(runningAuction);
  }

  // REGISTER AUTO-BID
  @Nested
  @DisplayName("register()")
  class RegisterTests {

    @Test
    @DisplayName("TC-AUTO-REG-01: DTO null → BAD_REQUEST")
    void register_nullDto() {
      Response res = autoBidService.register(null, "user-001");
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-REG-02: userId null → UNAUTHORIZED")
    void register_notLoggedIn() {
      Response res = autoBidService.register(new AutoBidDTO("auc-001", 2_000_000.0, 100_000.0), null);
      assertEquals(ResponseStatus.UNAUTHORIZED, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-REG-03: maxBid <= 0 → BAD_REQUEST")
    void register_invalidMaxBid() {
      Response res = autoBidService.register(new AutoBidDTO("auc-001", 0, 100_000.0), "user-001");
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-REG-04: increment <= 0 → BAD_REQUEST")
    void register_invalidIncrement() {
      Response res = autoBidService.register(new AutoBidDTO("auc-001", 2_000_000.0, 0), "user-001");
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-REG-05: Phiên không tồn tại → NOT_FOUND")
    void register_auctionNotFound() {
      Response res = autoBidService.register(new AutoBidDTO("ghost", 2_000_000.0, 100_000.0), "user-001");
      assertEquals(ResponseStatus.NOT_FOUND, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-REG-06: Phiên không đang chạy → BAD_REQUEST")
    void register_auctionNotRunning() {
      runningAuction.setStatus(AuctionStatus.FINISHED);
      Response res = autoBidService.register(new AutoBidDTO("auc-001", 2_000_000.0, 100_000.0), "user-001");
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-REG-07: maxBid <= currentPrice → BAD_REQUEST")
    void register_maxBidTooLow() {

      Response res = autoBidService.register(
          new AutoBidDTO("auc-001", 900_000.0, 50_000.0), "user-001");
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-REG-08: Đăng ký thành công → SUCCESS + lưu vào DB")
    void register_success() {
      Response res = autoBidService.register(
          new AutoBidDTO("auc-001", 3_000_000.0, 100_000.0), "user-001");
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertEquals(1, autoBidDAO.saveCount);
    }

    @Test
    @DisplayName("TC-AUTO-REG-09: Đăng ký lại (ghi đè) → vẫn SUCCESS, thay thế entry cũ")
    void register_override() {
      autoBidService.register(new AutoBidDTO("auc-001", 3_000_000.0, 100_000.0), "user-001");
      Response res = autoBidService.register(
          new AutoBidDTO("auc-001", 5_000_000.0, 200_000.0), "user-001");
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertEquals(2, autoBidDAO.saveCount);
    }
  }

  // CANCEL AUTO-BID
  @Nested
  @DisplayName("cancel()")
  class CancelTests {

    @Test
    @DisplayName("TC-AUTO-CANCEL-01: auctionId null → BAD_REQUEST")
    void cancel_nullAuctionId() {
      Response res = autoBidService.cancel(null, "user-001");
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-CANCEL-02: userId null → BAD_REQUEST")
    void cancel_nullUserId() {
      Response res = autoBidService.cancel("auc-001", null);
      assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-CANCEL-03: Hủy khi chưa đăng ký → NOT_FOUND")
    void cancel_notRegistered() {
      Response res = autoBidService.cancel("auc-001", "user-999");
      assertEquals(ResponseStatus.NOT_FOUND, res.getStatus());
    }

    @Test
    @DisplayName("TC-AUTO-CANCEL-04: Hủy thành công sau khi đã đăng ký")
    void cancel_success() {
      autoBidService.register(new AutoBidDTO("auc-001", 2_000_000.0, 100_000.0), "user-001");
      Response res = autoBidService.cancel("auc-001", "user-001");
      assertEquals(ResponseStatus.SUCCESS, res.getStatus());
      assertTrue(autoBidDAO.deleteCount > 0);
    }
  }


}
