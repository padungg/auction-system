package com.auction.model;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho Auction entity.
 * Tập trung vào logic Anti-Sniping được cài trong applyAntiSniping().
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
    @DisplayName("TC-ENTITY-02: applyAntiSniping trong 60s cuối → gia hạn thêm 120s")
    void antiSniping_withinThreshold() {
        Auction a = auctionWithSecondsLeft(30); // còn 30 giây
        LocalDateTime endBefore = a.getEndTime();

        boolean applied = a.applyAntiSniping();

        assertTrue(applied);
        // endTime phải tăng thêm 120 giây
        assertEquals(endBefore.plusSeconds(120), a.getEndTime());
    }

    @Test
    @DisplayName("TC-ENTITY-03: applyAntiSniping khi còn đúng 60s → gia hạn")
    void antiSniping_exactlyThreshold() {
        Auction a = auctionWithSecondsLeft(60);
        boolean applied = a.applyAntiSniping();
        assertTrue(applied);
    }

    @Test
    @DisplayName("TC-ENTITY-04: applyAntiSniping còn > 60s → KHÔNG gia hạn")
    void antiSniping_outsideThreshold() {
        Auction a = auctionWithSecondsLeft(65);
        LocalDateTime endBefore = a.getEndTime();

        boolean applied = a.applyAntiSniping();

        assertFalse(applied);
        assertEquals(endBefore, a.getEndTime()); // không thay đổi
    }

    @Test
    @DisplayName("TC-ENTITY-05: applyAntiSniping đã hết hạn → KHÔNG gia hạn (secondsLeft <= 0)")
    void antiSniping_expired() {
        Auction a = new Auction("auc-test", "item-test", 1_000_000.0,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusSeconds(5)); // đã quá hạn
        boolean applied = a.applyAntiSniping();
        assertFalse(applied);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 30, 59, 60})
    @DisplayName("TC-ENTITY-06: applyAntiSniping với nhiều giá trị trong threshold → luôn gia hạn")
    void antiSniping_variousSecondsWithinThreshold(long seconds) {
        Auction a = auctionWithSecondsLeft(seconds);
        assertTrue(a.applyAntiSniping(),
                "Phải gia hạn khi còn " + seconds + " giây");
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
