package com.auction.server.dao;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;

import java.util.List;

/**
 * Interface DAO cho Auction — "Hợp đồng" cho tầng Database.
 */
public interface AuctionDAO {

    /**
     * Lấy danh sách phiên đấu giá theo trạng thái.
     * VD: findAllByStatus(OPENING) → tất cả phiên đang mở.
     */
    List<Auction> findAllByStatus(AuctionStatus status);

    /**
     * Tìm phiên đấu giá theo ID.
     * @return Auction nếu tìm thấy, null nếu không tồn tại
     */
    Auction findById(String id);

    /**
     * Lưu phiên đấu giá mới.
     * @return true nếu lưu thành công
     */
    boolean save(Auction auction);

    /**
     * Cập nhật phiên đấu giá (giá hiện tại, winner, trạng thái).
     * Được gọi khi có người bid hoặc phiên kết thúc.
     * @return true nếu cập nhật thành công
     */
    boolean update(Auction auction);

    /**
     * Xóa phiên đấu giá theo ID.
     * @return true nếu xóa thành công
     */
    boolean delete(String id);
}

