package com.auction.server.dao;

import com.auction.model.entity.BidTransaction;
import com.auction.server.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAOImpl implements BidTransactionDAO {

    @Override
    public boolean save(BidTransaction bid) {
        String sql = "INSERT INTO bid_transactions (id, bidder_id, auction_id, bid_amount, bid_time, is_auto_bid) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, bid.getId());
            stmt.setString(2, bid.getBidderId());
            stmt.setString(3, bid.getAuctionId());
            stmt.setDouble(4, bid.getBidAmount());
            stmt.setTimestamp(5, Timestamp.valueOf(bid.getBidTime()));
            stmt.setBoolean(6, false);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println(">>> [BidTransactionDAO] Lỗi save: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<BidTransaction> findByAuctionId(String auctionId) {
        List<BidTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction();
                    bid.setId(rs.getString("id"));
                    bid.setBidderId(rs.getString("bidder_id"));
                    bid.setAuctionId(rs.getString("auction_id"));
                    bid.setBidAmount(rs.getDouble("bid_amount"));
                    bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
                    // bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    transactions.add(bid);
                }
            }
        } catch (SQLException e) {
            System.err.println(">>> [BidTransactionDAO] Lỗi findByAuctionId: " + e.getMessage());
        }
        return transactions;
    }

    @Override
    public List<BidTransaction> findByBidderId(String bidderId) {
        List<BidTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE bidder_id = ? ORDER BY bid_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction();
                    bid.setId(rs.getString("id"));
                    bid.setBidderId(rs.getString("bidder_id"));
                    bid.setAuctionId(rs.getString("auction_id"));
                    bid.setBidAmount(rs.getDouble("bid_amount"));
                    bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
                    // bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    transactions.add(bid);
                }
            }
        } catch (SQLException e) {
            System.err.println(">>> [BidTransactionDAO] Lỗi findByBidderId: " + e.getMessage());
        }
        return transactions;
    }
}
