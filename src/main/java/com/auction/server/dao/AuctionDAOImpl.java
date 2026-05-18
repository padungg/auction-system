package com.auction.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.model.entity.Auction;
import com.auction.model.entity.AuctionStatus;
import com.auction.server.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAOImpl implements AuctionDAO {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDAOImpl.class);


    @Override
    public List<Auction> findAllByStatus(AuctionStatus status) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(">>> [AuctionDAO] Lỗi findAllByStatus: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuction(rs);
                }
            }
        } catch (SQLException e) {
            logger.error(">>> [AuctionDAO] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean save(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItemId());
            stmt.setString(3, auction.getCurrentWinnerId());
            stmt.setDouble(4, auction.getCurrentPrice());
            stmt.setTimestamp(5, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(6, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(7, auction.getStatus().name());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error(">>> [AuctionDAO] Lỗi save: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean update(Auction auction) {
        String sql = "UPDATE auctions SET current_winner_id = ?, current_price = ?, status = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auction.getCurrentWinnerId());
            stmt.setDouble(2, auction.getCurrentPrice());
            stmt.setString(3, auction.getStatus().name());
            stmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(5, auction.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error(">>> [AuctionDAO] Lỗi update: " + e.getMessage());
        }
        return false;
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getString("id"));
        auction.setItemId(rs.getString("item_id"));
        auction.setCurrentWinnerId(rs.getString("current_winner_id"));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        
        String statusStr = rs.getString("status");
        try {
            auction.setStatus(AuctionStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            logger.error(">>> [AuctionDAO] Cảnh báo: Trạng thái không hợp lệ trong DB: " + statusStr + ". Đã đổi thành CANCELED.");
            auction.setStatus(AuctionStatus.CANCELED);
        }
        
        return auction;
    }

    @Override
    public boolean delete(String id) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error(">>> [AuctionDAO] Lỗi delete: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<Auction> findAll() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                auctions.add(mapResultSetToAuction(rs));
            }
        } catch (SQLException e) {
            logger.error(">>> [AuctionDAO] Lỗi findAll: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> findByCurrentWinnerId(String winnerId) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE current_winner_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, winnerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(">>> [AuctionDAO] Lỗi findByCurrentWinnerId: " + e.getMessage());
        }
        return auctions;
    }
}
