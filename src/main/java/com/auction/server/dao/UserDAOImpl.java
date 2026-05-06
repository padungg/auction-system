package com.auction.server.dao;

import com.auction.model.entity.User;
import com.auction.model.entity.UserRole;
import com.auction.server.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Triển khai UserDAO kết nối MySQL thực tế.
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println(">>> [UserDAO] Lỗi findByUsername: " + e.getMessage());
        }
        return null;
    }

    @Override
    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println(">>> [UserDAO] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean save(User user) {
        String sql = "INSERT INTO users (id, username, password, email, full_name, phone, address, " +
                     "is_active, role, balance, store_name, rating) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1,  user.getId());
            stmt.setString(2,  user.getUsername());
            stmt.setString(3,  user.getPassword());
            stmt.setString(4,  user.getEmail());
            stmt.setString(5,  user.getFullName());
            stmt.setString(6,  user.getPhone());
            stmt.setString(7,  user.getAddress());
            stmt.setBoolean(8, user.isActive());
            stmt.setString(9,  user.getRole().name());
            stmt.setDouble(10, user.getBalance());
            stmt.setString(11, user.getStoreName());
            stmt.setDouble(12, user.getRating());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> [UserDAO] Lỗi save: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println(">>> [UserDAO] Lỗi existsByUsername: " + e.getMessage());
        }
        return false;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getBoolean("is_active"),
                UserRole.valueOf(rs.getString("role")),
                rs.getDouble("balance"),
                rs.getString("store_name"),
                rs.getDouble("rating")
        );
    }
}
