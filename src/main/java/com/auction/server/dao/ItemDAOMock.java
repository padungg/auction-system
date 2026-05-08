package com.auction.server.dao;

import com.auction.model.entity.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation của ItemDAO — dùng ArrayList thay cho MySQL.
 */
public class ItemDAOMock implements ItemDAO {

    private final List<Item> items = new ArrayList<>();

    public ItemDAOMock() {
        System.out.println(">>> [ItemDAOMock] Khởi tạo (rỗng, sẽ thêm khi tạo phiên đấu giá)");
    }

    @Override
    public Item findById(String id) {
        return items.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean save(Item item) {
        return items.add(item);
    }

    @Override
    public boolean update(Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                items.set(i, item);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(String id) {
        return items.removeIf(item -> item.getId().equals(id));
    }
}
