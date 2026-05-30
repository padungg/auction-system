package com.auction.server.service;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.model.entity.AutoBidEntry;
import com.auction.model.entity.Item;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.ItemDAO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nghiệp vụ Đặt giá tự động (Auto-Bid).
 */
public class AutoBidService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBidService.class);

    private final AuctionDAO auctionDAO;
    private final AutoBidDAO autoBidDAO;
    private final ItemDAO itemDAO;
    private BidService bidService;

    private final Map<String, PriorityQueue<AutoBidEntry>> registry = new ConcurrentHashMap<>();

    public AutoBidService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO, ItemDAO itemDAO) {
        this.auctionDAO = auctionDAO;
        this.autoBidDAO = autoBidDAO;
        this.itemDAO = itemDAO;
        loadFromDB();
    }

    public void setBidService(BidService bidService) {
        this.bidService = bidService;
    }

    private void loadFromDB() {
        List<AutoBidEntry> entries = autoBidDAO.findAll();
        for (AutoBidEntry entry : entries) {
            PriorityQueue<AutoBidEntry> queue = registry.computeIfAbsent(
                    entry.getAuctionId(), ignored -> new PriorityQueue<>());
            queue.add(entry);
        }
        LOGGER.info("Đã load {} auto-bids từ DB.", entries.size());
    }



    /** Đăng ký Auto-Bid. */
    public Response register(AutoBidDTO dto, String userId) {
        if (dto == null || dto.getAuctionId() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin auto-bid", null);
        }

        Object lock = com.auction.server.util.LockManager.getAuctionLock(dto.getAuctionId());
        synchronized (lock) {
            if (userId == null) {
                return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
            }
            if (dto.getMaxBid() <= 0) {
                return new Response(ResponseStatus.BAD_REQUEST, "Giá tối đa phải lớn hơn 0", null);
            }
            if (dto.getIncrement() <= 0) {
                return new Response(ResponseStatus.BAD_REQUEST, "Bước tăng giá phải lớn hơn 0", null);
            }

            Auction auction = auctionDAO.findById(dto.getAuctionId());
            if (auction == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Phiên đấu giá không tồn tại", null);
            }
            if (dto.getIncrement() < auction.getStepPrice()) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Bước tăng giá phải lớn hơn hoặc bằng bước giá tối thiểu của phiên: "
                                + String.format("%,.0f", auction.getStepPrice()) + " VNĐ",
                        null);
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Chỉ có thể đăng ký auto-bid cho phiên đang mở", null);
            }
            if (dto.getMaxBid() <= auction.getCurrentPrice()) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Giá tối đa phải cao hơn giá hiện tại: "
                                + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ",
                        null);
            }

            // Không cho chủ sản phẩm tự đăng ký auto-bid
            Item item = itemDAO.findById(auction.getItemId());
            if (item != null && userId.equals(item.getSellerId())) {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Bạn không thể đăng ký auto-bid cho sản phẩm của chính mình!", null);
            }

            PriorityQueue<AutoBidEntry> queue = registry.computeIfAbsent(
                    dto.getAuctionId(), ignored -> new PriorityQueue<>());

            AutoBidEntry oldEntry = null;
            for (AutoBidEntry e : queue) {
                if (e.getUserId().equals(userId)) {
                    oldEntry = e;
                    break;
                }
            }

            AutoBidEntry newEntry = new AutoBidEntry(userId, dto.getAuctionId(), dto.getMaxBid(), dto.getIncrement());

            boolean saved = autoBidDAO.save(newEntry);
            if (!saved) {
                if (queue.isEmpty()) {
                    registry.remove(dto.getAuctionId());
                }
                return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể lưu đăng ký auto-bid vào Database",
                        null);
            }

            if (oldEntry != null) {
                queue.remove(oldEntry);
            }
            queue.add(newEntry);

            LOGGER.info("REGISTER: phiên={} | user={} | maxBid={} | increment={} | tổng auto-bidder={}",
                    dto.getAuctionId(),
                    userId,
                    String.format("%,.0f", dto.getMaxBid()),
                    String.format("%,.0f", dto.getIncrement()),
                    queue.size());

            if (bidService != null) {
                bidService.runAutoBids(dto.getAuctionId());
            }

            return new Response(ResponseStatus.SUCCESS,
                    "Đăng ký auto-bid thành công! Hệ thống đã bắt đầu tự động đặt giá cho bạn.", null);
        }
    }

    /** Hủy Auto-Bid. */
    public Response cancel(String auctionId, String userId) {
        if (auctionId == null || userId == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin hủy auto-bid", null);
        }

        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            PriorityQueue<AutoBidEntry> queue = registry.get(auctionId);
            if (queue == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Bạn chưa đăng ký auto-bid cho phiên này", null);
            }

            AutoBidEntry target = null;
            for (AutoBidEntry e : queue) {
                if (e.getUserId().equals(userId)) {
                    target = e;
                    break;
                }
            }

            if (target == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Bạn chưa đăng ký auto-bid cho phiên này", null);
            }

            boolean deleted = autoBidDAO.delete(auctionId, userId);
            if (!deleted) {
                return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể hủy đăng ký auto-bid trong Database",
                        null);
            }

            queue.remove(target);
            if (queue.isEmpty()) {
                registry.remove(auctionId);
            }

            LOGGER.info("CANCEL: phiên={} | user={}", auctionId, userId);
            return new Response(ResponseStatus.SUCCESS, "Đã hủy đăng ký auto-bid", null);
        }
    }

    /** Xóa toàn bộ auto-bid của phiên khi đóng. */
    public void clearAuction(String auctionId) {
        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            boolean success = autoBidDAO.deleteByAuctionId(auctionId);
            if (success) {
                registry.remove(auctionId);
                LOGGER.info("CLEAR: đã xóa auto-bid của phiên={}", auctionId);
            } else {
                LOGGER.error("CLEAR: không thể xóa auto-bid của phiên={} trong Database", auctionId);
            }
        }
    }




    public record NextAutoBid(String userId, double bidAmount) {
    }

    /** Tính toán lượt auto-bid tiếp theo và giá đặt mới. */
    public NextAutoBid calculateNextAutoBid(String auctionId, double currentPrice, String currentWinnerId) {
        Object lock = com.auction.server.util.LockManager.getAuctionLock(auctionId);
        synchronized (lock) {
            PriorityQueue<AutoBidEntry> queue = registry.get(auctionId);
            if (queue == null || queue.isEmpty())
                return null;

            List<AutoBidEntry> sorted = new ArrayList<>(queue);
            Collections.sort(sorted);

            List<AutoBidEntry> exhausted = new ArrayList<>();
            AutoBidEntry winner = null;

            for (AutoBidEntry entry : sorted) {
                double nextBid = currentPrice + entry.getIncrement();

                if (nextBid > entry.getMaxBid()) {
                    exhausted.add(entry);
                    continue;
                }

                if (entry.getUserId().equals(currentWinnerId)) {
                    continue;
                }

                winner = entry;
                break;
            }

            queue.removeAll(exhausted);
            if (!exhausted.isEmpty()) {
                for (AutoBidEntry e : exhausted) {
                    LOGGER.info("EXHAUSTED: user={} | maxBid={} vượt ngưỡng — loại khỏi queue",
                            e.getUserId(),
                            String.format("%,.0f", e.getMaxBid()));
                }
            }

            if (winner != null) {
                return new NextAutoBid(winner.getUserId(), currentPrice + winner.getIncrement());
            }

            return null;
        }
    }
}