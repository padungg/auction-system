package com.auction.server.service;

import com.auction.server.service.AutoBidEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho AutoBidEntry.
 * Kiểm tra FCFS ordering thông qua Comparable + PriorityQueue.
 */
@DisplayName("AutoBidEntry Tests")
class AutoBidEntryTest {

    @Test
    @DisplayName("TC-ENTRY-01: compareTo → người đăng ký trước nhỏ hơn (ưu tiên trong PQ)")
    void compareTo_earlierRegistrationHasPriority() {
        LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2025, 1, 1, 10, 5, 0);

        AutoBidEntry early = new AutoBidEntry("user-A", "auc-1", 3_000_000.0, 100_000.0, t1);
        AutoBidEntry late = new AutoBidEntry("user-B", "auc-1", 3_000_000.0, 100_000.0, t2);

        assertTrue(early.compareTo(late) < 0, "Người đăng ký trước phải nhỏ hơn");
        assertTrue(late.compareTo(early) > 0, "Người đăng ký sau phải lớn hơn");
    }

    @Test
    @DisplayName("TC-ENTRY-02: compareTo cùng thời điểm → bằng 0")
    void compareTo_sameTime_equalsZero() {
        LocalDateTime t = LocalDateTime.of(2025, 6, 15, 9, 0, 0);
        AutoBidEntry a = new AutoBidEntry("u1", "auc", 1_000.0, 100.0, t);
        AutoBidEntry b = new AutoBidEntry("u2", "auc", 2_000.0, 200.0, t);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    @DisplayName("TC-ENTRY-03: PriorityQueue → poll() trả về người đăng ký sớm nhất trước")
    void priorityQueue_pollsInFcfsOrder() {
        LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 8, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2025, 1, 1, 9, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);

        AutoBidEntry e1 = new AutoBidEntry("user-A", "auc", 1_000.0, 100.0, t1);
        AutoBidEntry e2 = new AutoBidEntry("user-B", "auc", 1_000.0, 100.0, t2);
        AutoBidEntry e3 = new AutoBidEntry("user-C", "auc", 1_000.0, 100.0, t3);

        PriorityQueue<AutoBidEntry> pq = new PriorityQueue<>();
        pq.add(e3); // thêm theo thứ tự ngẫu nhiên
        pq.add(e1);
        pq.add(e2);

        // PQ phải trả về theo thứ tự FCFS
        assertEquals("user-A", pq.poll().getUserId());
        assertEquals("user-B", pq.poll().getUserId());
        assertEquals("user-C", pq.poll().getUserId());
    }

    @Test
    @DisplayName("TC-ENTRY-04: Getters trả về đúng giá trị")
    void getters_returnCorrectValues() {
        LocalDateTime t = LocalDateTime.of(2025, 5, 10, 12, 30, 0);
        AutoBidEntry e = new AutoBidEntry("user-X", "auction-Y", 5_000_000.0, 250_000.0, t);

        assertEquals("user-X", e.getUserId());
        assertEquals("auction-Y", e.getAuctionId());
        assertEquals(5_000_000.0, e.getMaxBid(), 0.001);
        assertEquals(250_000.0, e.getIncrement(), 0.001);
        assertEquals(t, e.getRegisteredAt());
    }

    @Test
    @DisplayName("TC-ENTRY-05: Constructor không có registeredAt → tự set thời gian hiện tại")
    void constructor_withoutTime_setsNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        AutoBidEntry e = new AutoBidEntry("u1", "auc-1", 1_000.0, 100.0);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(e.getRegisteredAt());
        assertTrue(e.getRegisteredAt().isAfter(before) || e.getRegisteredAt().isEqual(before),
                "registeredAt phải >= before");
        assertTrue(e.getRegisteredAt().isBefore(after) || e.getRegisteredAt().isEqual(after),
                "registeredAt phải <= after");
    }

    @Test
    @DisplayName("TC-ENTRY-06: Sort danh sách theo FCFS")
    void sortList_fcfsOrder() {
        LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2025, 1, 1, 10, 5, 0);
        LocalDateTime t3 = LocalDateTime.of(2025, 1, 1, 10, 2, 0);

        AutoBidEntry e1 = new AutoBidEntry("A", "auc", 1_000.0, 100.0, t1);
        AutoBidEntry e2 = new AutoBidEntry("B", "auc", 1_000.0, 100.0, t2);
        AutoBidEntry e3 = new AutoBidEntry("C", "auc", 1_000.0, 100.0, t3);

        List<AutoBidEntry> list = new ArrayList<>(List.of(e2, e3, e1));
        Collections.sort(list);

        assertEquals("A", list.get(0).getUserId()); // t1 sớm nhất
        assertEquals("C", list.get(1).getUserId()); // t3
        assertEquals("B", list.get(2).getUserId()); // t2 muộn nhất
    }
}
