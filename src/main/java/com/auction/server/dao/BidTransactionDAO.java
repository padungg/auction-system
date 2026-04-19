package com.auction.server.dao;

import com.auction.model.entity.BidTransaction;

import java.util.List;

/**
 * Interface DAO cho BidTransaction — "Hợp đồng" cho tầng Database.
 */
public interface BidTransactionDAO {

    /**
     * Lưu giao dịch đặt giá mới.
     * @return true nếu lưu thành công
     */
    boolean save(BidTransaction bid);

    /**
     * Lấy tất cả lịch sử bid của một phiên đấu giá.
     * Dùng để hiển thị lịch sử đặt giá trong trang chi tiết.
     */
    List<BidTransaction> findByAuctionId(String auctionId);
}
