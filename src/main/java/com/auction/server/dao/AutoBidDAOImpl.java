package com.auction.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.server.database.DatabaseConnection;
import com.auction.model.entity.AutoBidEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAOImpl implements AutoBidDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBidDAOImpl.class);

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
            LOGGER.error(">>> [AutoBidDAO] Lỗi findAll: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(AutoBidEntry entry) {
        String sql = "INSERT INTO auto_bids (user_id, auction_id, max_bid, increment, registered_at) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (user_id, auction_id) DO UPDATE SET " +
                     "max_bid = EXCLUDED.max_bid, increment = EXCLUDED.increment, registered_at = EXCLUDED.registered_at";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getUserId());
            stmt.setString(2, entry.getAuctionId());
            stmt.setDouble(3, entry.getMaxBid());
            stmt.setDouble(4, entry.getIncrement());
            stmt.setTimestamp(5, Timestamp.valueOf(entry.getRegisteredAt()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error(">>> [AutoBidDAO] Lỗi save: {}", e.getMessage(), e);
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
            LOGGER.error(">>> [AutoBidDAO] Lỗi delete: {}", e.getMessage(), e);
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
            LOGGER.error(">>> [AutoBidDAO] Lỗi deleteByAuctionId: {}", e.getMessage(), e);
        }
        return false;
    }
}
