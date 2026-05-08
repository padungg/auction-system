package com.auction.server.dao;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.service.AutoBidEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAOImpl implements AutoBidDAO {

    @Override
    public List<AutoBidEntry> findAll() {
        List<AutoBidEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM auto_bids";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new AutoBidEntry(
                        rs.getString("user_id"),
                        rs.getString("auction_id"),
                        rs.getDouble("max_bid"),
                        rs.getDouble("increment"),
                        rs.getTimestamp("registered_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println(">>> [AutoBidDAO] Lỗi findAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean save(AutoBidEntry entry) {
        String sql = "INSERT INTO auto_bids (user_id, auction_id, max_bid, increment, registered_at) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE max_bid = VALUES(max_bid), increment = VALUES(increment), registered_at = VALUES(registered_at)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getUserId());
            stmt.setString(2, entry.getAuctionId());
            stmt.setDouble(3, entry.getMaxBid());
            stmt.setDouble(4, entry.getIncrement());
            stmt.setTimestamp(5, Timestamp.valueOf(entry.getRegisteredAt()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> [AutoBidDAO] Lỗi save: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(String auctionId, String userId) {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);
            stmt.setString(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> [AutoBidDAO] Lỗi delete: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteByAuctionId(String auctionId) {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> [AutoBidDAO] Lỗi deleteByAuctionId: " + e.getMessage());
        }
        return false;
    }
}
