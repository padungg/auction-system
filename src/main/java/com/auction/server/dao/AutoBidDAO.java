package com.auction.server.dao;

import com.auction.server.service.AutoBidEntry;
import java.util.List;

public interface AutoBidDAO {
    List<AutoBidEntry> findAll();
    boolean save(AutoBidEntry entry);
    boolean delete(String auctionId, String userId);
    boolean deleteByAuctionId(String auctionId);
}
