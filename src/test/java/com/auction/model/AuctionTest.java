package com.auction.model;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
* Unit tests cho Auction entity.
*/
@DisplayName("Auction Entity Tests")
class AuctionTest {

  /**
  * Tạo Auction với endTime = now + giây
  */
  private Auction auctionWithSecondsLeft(long secondsLeft) {
    LocalDateTime end = LocalDateTime.now().plusSeconds(secondsLeft);
    Auction a = new Auction("auc-test", "item-test", 1_000_000.0,
        LocalDateTime.now().minusHours(1), end);
    a.setStatus(AuctionStatus.RUNNING);
    return a;
  }

  @Test
  @DisplayName("TC-ENTITY-01: Constructor → status mặc định là OPEN")
  void constructor_defaultStatus() {
    Auction a = new Auction("id-1", "item-1", 500_000.0,
        LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    assertEquals(AuctionStatus.OPEN, a.getStatus());
  }

  @Test
  @DisplayName("TC-ENTITY-02: Constructor → khởi tạo đúng ID, itemId, giá ban đầu")
  void constructor_fieldsInitializedCorrectly() {
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusDays(3);
    Auction a = new Auction("auc-id", "item-id", 2_000_000.0, start, end);

    assertEquals("auc-id", a.getId());
    assertEquals("item-id", a.getItemId());
    assertEquals(2_000_000.0, a.getCurrentPrice(), 0.001);
    assertEquals(start, a.getStartTime());
    assertEquals(end, a.getEndTime());
    assertNull(a.getCurrentWinnerId());
  }

  @Test
  @DisplayName("TC-ENTITY-03: currentWinnerId mặc định = null")
  void constructor_winnerIdDefaultNull() {
    Auction a = new Auction("id", "item", 1_000.0,
        LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    assertNull(a.getCurrentWinnerId());
  }

  @Test
  @DisplayName("TC-ENTITY-07: setStatus → getStatus trả đúng giá trị")
  void setStatus_updatesCorrectly() {
    Auction a = auctionWithSecondsLeft(3600);
    a.setStatus(AuctionStatus.FINISHED);
    assertEquals(AuctionStatus.FINISHED, a.getStatus());
  }

  @Test
  @DisplayName("TC-ENTITY-08: setCurrentPrice → getPrice phản ánh đúng")
  void setCurrentPrice() {
    Auction a = auctionWithSecondsLeft(3600);
    a.setCurrentPrice(5_000_000.0);
    assertEquals(5_000_000.0, a.getCurrentPrice(), 0.001);
  }

  @Test
  @DisplayName("TC-ENTITY-09: setCurrentWinnerId → getWinnerId phản ánh đúng")
  void setCurrentWinnerId() {
    Auction a = auctionWithSecondsLeft(3600);
    assertNull(a.getCurrentWinnerId()); // ban đầu null
    a.setCurrentWinnerId("user-abc");
    assertEquals("user-abc", a.getCurrentWinnerId());
  }
}
