package com.auction.server.dao;

import com.auction.model.entity.Item;

/**
 * Interface DAO cho Item — "Hợp đồng" cho tầng Database.
 * Áp dụng chung cho mọi loại Item (Vehicle, Electronics, Art) nhờ tính đa hình.
 */
public interface ItemDAO {

    /**
     * Tìm item theo ID.
     * @return Item (có thể là Vehicle/Electronics/Art), null nếu không tồn tại
     */
    Item findById(String id);

    /**
     * Lưu item mới vào database (khi tạo phiên đấu giá).
     * @return true nếu lưu thành công
     */
    boolean save(Item item);
}
