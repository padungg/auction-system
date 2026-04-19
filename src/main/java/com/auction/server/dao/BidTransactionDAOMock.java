package com.auction.server.dao;

import com.auction.model.entity.BidTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock implementation của BidTransactionDAO — dùng ArrayList thay cho MySQL.
 */
public class BidTransactionDAOMock implements BidTransactionDAO {

    private final List<BidTransaction> transactions = new ArrayList<>();

    public BidTransactionDAOMock() {
        System.out.println(">>> [BidTransactionDAOMock] Khởi tạo (rỗng)");
    }

    @Override
    public boolean save(BidTransaction bid) {
        return transactions.add(bid);
    }

    @Override
    public List<BidTransaction> findByAuctionId(String auctionId) {
        return transactions.stream()
                .filter(t -> t.getAuctionId().equals(auctionId))
                .collect(Collectors.toList());
    }
}
