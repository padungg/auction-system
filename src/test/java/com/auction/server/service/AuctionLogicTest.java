package com.auction.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test mẫu cho logic nghiệp vụ đấu giá. */
public class AuctionLogicTest {

  @BeforeEach
  void setUp() {
  }

  @Test
  void testValidBid_ShouldReturnTrue_WhenBidIsHigherThanCurrentPrice() {
    double currentPrice = 100.0;
    double increment = 10.0;
    double newBid = 120.0;

    boolean isValid = checkBidValidity(currentPrice, increment, newBid);

    assertTrue(isValid, "Giá đấu cao hơn giá hiện tại + bước giá, phải hợp lệ");
  }

  @Test
  void testInvalidBid_ShouldReturnFalse_WhenBidIsTooLow() {
    double currentPrice = 100.0;
    double increment = 10.0;
    double newBid = 105.0;

    boolean isValid = checkBidValidity(currentPrice, increment, newBid);

    assertFalse(isValid, "Giá đấu quá thấp, không hợp lệ");
  }

  @Test
  void testAntiSniping_ShouldExtendEndTime_WhenBidPlacedNearClosing() {
    long closingTime = 100000;
    long currentTime = 99980;
    
    long newClosingTime = extendAuctionTimeIfSniping(currentTime, closingTime, 30, 60);
    
    assertEquals(100060, newClosingTime, "Thời gian kết thúc phải được gia hạn thêm 60 giây");
  }



  private boolean checkBidValidity(double currentPrice, double increment, double newBid) {
    return newBid >= (currentPrice + increment);
  }

  private long extendAuctionTimeIfSniping(long currentTime, long closingTime, long thresholdSeconds, long extensionSeconds) {
    long remainingTime = closingTime - currentTime;
    if (remainingTime > 0 && remainingTime <= thresholdSeconds) {
      return closingTime + extensionSeconds;
    }
    return closingTime;
  }
}
