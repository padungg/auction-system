package com.auction.server.util;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.server.dao.AuctionDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests cho AuctionUtils. */
@DisplayName("AuctionUtils Tests")
class AuctionUtilsTest {


  static class AuctionDAOStub implements AuctionDAO {
    int updateCount = 0;
    Auction lastUpdatedAuction = null;

    @Override
    public List<Auction> findAllByStatus(AuctionStatus status) {
      return null;
    }

    @Override
    public Auction findById(String id) {
      return null;
    }

    @Override
    public boolean save(Auction auction) {
      return true;
    }

    @Override
    public boolean update(Auction auction) {
      updateCount++;
      lastUpdatedAuction = auction;
      return true;
    }

    @Override
    public boolean delete(String id) {
      return true;
    }

    @Override
    public List<Auction> findAll() {
      return new java.util.ArrayList<>();
    }

    @Override
    public List<Auction> findByCurrentWinnerId(String winnerId) {
      return new java.util.ArrayList<>();
    }
  }

  private Auction buildAuction(long secondsUntilEnd) {
    Auction a = new Auction("auc-001", "item-001", 1_000_000.0,
        LocalDateTime.now().minusHours(1),
        LocalDateTime.now().plusSeconds(secondsUntilEnd));
    a.setStatus(AuctionStatus.RUNNING);
    return a;
  }

  @Test
  @DisplayName("TC-UTILS-01: Còn <= 30s → gia hạn thêm 60s + cập nhật DB")
  void applyAntiSnipe_withinWindow_extends() {
    Auction auction = buildAuction(15); // còn 15 giây
    LocalDateTime endBefore = auction.getEndTime();
    AuctionDAOStub daoStub = new AuctionDAOStub();

    AuctionUtils.applyAntiSnipe(auction, daoStub);

    assertEquals(endBefore.plusSeconds(60), auction.getEndTime());
    assertEquals(1, daoStub.updateCount);
    assertEquals(auction, daoStub.lastUpdatedAuction);
  }

  @Test
  @DisplayName("TC-UTILS-02: Còn đúng 30s → gia hạn")
  void applyAntiSnipe_exactlyAtThreshold() {
    Auction auction = buildAuction(30);
    AuctionDAOStub daoStub = new AuctionDAOStub();

    AuctionUtils.applyAntiSnipe(auction, daoStub);

    assertEquals(1, daoStub.updateCount);
  }

  @Test
  @DisplayName("TC-UTILS-03: Còn > 30s → KHÔNG gia hạn, KHÔNG gọi update")
  void applyAntiSnipe_outsideWindow_noChange() {
    Auction auction = buildAuction(60); // còn 60 giây
    LocalDateTime endBefore = auction.getEndTime();
    AuctionDAOStub daoStub = new AuctionDAOStub();

    AuctionUtils.applyAntiSnipe(auction, daoStub);

    assertEquals(endBefore, auction.getEndTime());
    assertEquals(0, daoStub.updateCount);
  }

  @Test
  @DisplayName("TC-UTILS-04: Đã hết hạn (seconds <= 0) → KHÔNG gia hạn")
  void applyAntiSnipe_expired() {
    Auction auction = new Auction("auc-001", "item-001", 1_000_000.0,
        LocalDateTime.now().minusHours(2),
        LocalDateTime.now().minusSeconds(10)); // đã quá hạn
    LocalDateTime endBefore = auction.getEndTime();
    AuctionDAOStub daoStub = new AuctionDAOStub();

    AuctionUtils.applyAntiSnipe(auction, daoStub);

    assertEquals(endBefore, auction.getEndTime());
    assertEquals(0, daoStub.updateCount);
  }

  @Test
  @DisplayName("TC-UTILS-05: Gia hạn 2 lần liên tiếp → mỗi lần tăng thêm 60s")
  void applyAntiSnipe_appliedTwice() {
    Auction auction = buildAuction(15);
    LocalDateTime t0 = auction.getEndTime();
    AuctionDAOStub daoStub = new AuctionDAOStub();

    AuctionUtils.applyAntiSnipe(auction, daoStub); // t0 + 60
    AuctionUtils.applyAntiSnipe(auction, daoStub); // endTime mới = t0+60, còn 75s > 30 → KHÔNG gia hạn lần 2

    assertEquals(t0.plusSeconds(60), auction.getEndTime());
    assertEquals(1, daoStub.updateCount); // chỉ 1 lần update
  }
}
