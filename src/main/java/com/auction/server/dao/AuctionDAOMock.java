package com.auction.server.dao;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock implementation của AuctionDAO — dùng ArrayList thay cho MySQL.
 */
public class AuctionDAOMock implements AuctionDAO {

    private final List<Auction> auctions = new ArrayList<>();

    public AuctionDAOMock() {
        System.out.println(">>> [AuctionDAOMock] Khởi tạo (rỗng, sẽ thêm khi tạo phiên đấu giá)");
    }

    @Override
    public List<Auction> findAllByStatus(AuctionStatus status) {
        return auctions.stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public Auction findById(String id) {
        return auctions.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean save(Auction auction) {
        return auctions.add(auction);
    }

    @Override
    public boolean update(Auction auction) {
        // Trong mock, object đã được sửa trực tiếp trên RAM (reference)
        // nên không cần làm gì thêm. Trong DAO thật sẽ UPDATE vào MySQL.
        return true;
    }

    @Override
    public boolean delete(String id) {
        return auctions.removeIf(a -> a.getId().equals(id));
    }
}
