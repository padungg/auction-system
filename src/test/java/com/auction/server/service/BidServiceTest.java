package com.auction.server.service;

import com.auction.model.dto.BidRequestDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.BidTransaction;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.model.entity.Item;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho BidService.
 * Đã thay thế Mockito bằng manual stubs (in-memory DAOs) để tương thích Java
 * 25.
 */
@DisplayName("BidService Tests")
class BidServiceTest {

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
        public List<BidTransaction> bids = new ArrayList<>();

        @Override
        public boolean save(BidTransaction bid) {
            saveCount++;
            return true;
        }

        @Override
        public List<BidTransaction> findByAuctionId(String auctionId) {
            return bids;
        }

        @Override
        public List<BidTransaction> findByBidderId(String bidderId) {
            return new ArrayList<>();
        }
    }

    static class AutoBidDAOStub implements AutoBidDAO {
        @Override
        public List<AutoBidEntry> findAll() {
            return new ArrayList<>();
        }

        @Override
        public boolean save(AutoBidEntry entry) {
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

    static class ItemDAOStub implements ItemDAO {
        @Override
        public Item findById(String id) {
            return null;
        }

        @Override
        public boolean save(Item item) {
            return true;
        }

        @Override
        public boolean update(Item item) {
            return true;
        }

        @Override
        public boolean delete(String id) {
            return true;
        }
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

        autoBidService = new AutoBidService(auctionDAO, autoBidDAO);
        bidService = new BidService(auctionDAO, bidTransactionDAO, autoBidService, itemDAO);
        autoBidService.setBidService(bidService);

        runningAuction = new Auction("auc-001", "item-001", 1_000_000.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
        runningAuction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.addAuction(runningAuction);
    }

    // PLACE BID
    @Nested
    @DisplayName("placeBid()")
    class PlaceBidTests {

        @Test
        @DisplayName("TC-BID-01: DTO null → BAD_REQUEST")
        void placeBid_nullDto() {
            assertEquals(ResponseStatus.BAD_REQUEST, bidService.placeBid(null, "user-001").getStatus());
        }

        @Test
        @DisplayName("TC-BID-02: userId null → UNAUTHORIZED")
        void placeBid_nullUserId() {
            assertEquals(ResponseStatus.UNAUTHORIZED,
                    bidService.placeBid(new BidRequestDTO("auc-001", 1_500_000.0), null).getStatus());
        }

        @Test
        @DisplayName("TC-BID-03: Phiên đấu giá không tồn tại → NOT_FOUND")
        void placeBid_auctionNotFound() {
            assertEquals(ResponseStatus.NOT_FOUND,
                    bidService.placeBid(new BidRequestDTO("ghost", 1_500_000.0), "user-001").getStatus());
        }

        @Test
        @DisplayName("TC-BID-04: Phiên đã đóng (không phải RUNNING) → BAD_REQUEST")
        void placeBid_auctionNotRunning() {
            runningAuction.setStatus(AuctionStatus.FINISHED);
            assertEquals(ResponseStatus.BAD_REQUEST,
                    bidService.placeBid(new BidRequestDTO("auc-001", 1_500_000.0), "user-001").getStatus());
        }

        @Test
        @DisplayName("TC-BID-05: Đặt giá thấp hơn hoặc bằng giá hiện tại → BAD_REQUEST")
        void placeBid_bidTooLow() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                    bidService.placeBid(new BidRequestDTO("auc-001", 1_000_000.0), "user-001").getStatus());
        }

        @Test
        @DisplayName("TC-BID-06: Đặt giá hợp lệ → SUCCESS, giá thay đổi, ghi log")
        void placeBid_success() {
            Response res = bidService.placeBid(new BidRequestDTO("auc-001", 1_500_000.0), "user-001");

            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertEquals(1, bidTransactionDAO.saveCount);
            assertEquals(1, auctionDAO.updateCount);
            assertEquals(1_500_000.0, runningAuction.getCurrentPrice(), 0.001);
            assertEquals("user-001", runningAuction.getCurrentWinnerId());
        }

        @Test
        @DisplayName("TC-BID-07: Anti-sniping được kích hoạt khi bid vào phút chót")
        void placeBid_triggersAntiSniping() {
            Auction snipeAuction = new Auction("auc-002", "item-001", 1_000_000.0,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusSeconds(30));
            snipeAuction.setStatus(AuctionStatus.RUNNING);
            auctionDAO.addAuction(snipeAuction);

            LocalDateTime oldEndTime = snipeAuction.getEndTime();

            Response res = bidService.placeBid(new BidRequestDTO("auc-002", 1_500_000.0), "user-001");

            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertTrue(snipeAuction.getEndTime().isAfter(oldEndTime), "Phải gia hạn thời gian (Anti-Sniping)");
        }

        @Test
        @DisplayName("TC-BID-08: Số tiền đặt giá bằng 0 → BAD_REQUEST")
        void placeBid_amountZero() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                    bidService.placeBid(new BidRequestDTO("auc-001", 0), "user-001").getStatus());
        }

        @Test
        @DisplayName("TC-BID-09: Tự bid vào phiên mình đang top 1 → BAD_REQUEST")
        void placeBid_bidAgainstSelf() {
            runningAuction.setCurrentWinnerId("user-001");
            Response res = bidService.placeBid(new BidRequestDTO("auc-001", 5_000_000.0), "user-001");
            assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
            assertTrue(res.getMessage().contains("không cần bid thêm"));
        }

        @Test
        @DisplayName("TC-BID-10: Bid vào phiên đã quá giờ → auto FINISHED + BAD_REQUEST")
        void placeBid_auctionExpiredByTime() {
            Auction expiredAuction = new Auction("auc-expired", "item-001", 1_000_000.0,
                    LocalDateTime.now().minusHours(2), LocalDateTime.now().minusSeconds(5));
            expiredAuction.setStatus(AuctionStatus.RUNNING);
            auctionDAO.addAuction(expiredAuction);

            Response res = bidService.placeBid(new BidRequestDTO("auc-expired", 5_000_000.0), "user-001");

            assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
            assertEquals(AuctionStatus.FINISHED, expiredAuction.getStatus());
            assertTrue(auctionDAO.updateCount > 0, "Phải cập nhật DB khi tự đóng phiên");
        }
    }

    // GET BID HISTORY
    @Nested
    @DisplayName("getBidHistory()")
    class GetBidHistoryTests {

        @Test
        @DisplayName("TC-BID-HIST-01: auctionId rỗng → BAD_REQUEST")
        void getHistory_emptyAuctionId() {
            assertEquals(ResponseStatus.BAD_REQUEST, bidService.getBidHistory(null).getStatus());
            assertEquals(ResponseStatus.BAD_REQUEST, bidService.getBidHistory("   ").getStatus());
        }

        @Test
        @DisplayName("TC-BID-HIST-02: Lấy lịch sử thành công → SUCCESS + List<BidTransaction>")
        void getHistory_success() {
            bidTransactionDAO.bids = new ArrayList<>(List.of(
                    new BidTransaction("bid-1", "u1", "auc-001", 1_100_000.0, LocalDateTime.now()),
                    new BidTransaction("bid-2", "u2", "auc-001", 1_200_000.0, LocalDateTime.now())));

            Response res = bidService.getBidHistory("auc-001");
            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertInstanceOf(List.class, res.getPayload());

            @SuppressWarnings("unchecked")
            List<BidTransaction> list = (List<BidTransaction>) res.getPayload();
            assertEquals(2, list.size());
        }
    }
}
