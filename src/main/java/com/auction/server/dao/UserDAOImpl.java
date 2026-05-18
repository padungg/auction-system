package com.auction.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.model.entity.User;
import com.auction.model.entity.UserRole;
import com.auction.server.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai UserDAO kết nối MySQL thực tế.
 */
public class UserDAOImpl implements UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);


    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error(">>> [UserDAO] Lỗi findByUsername: " + e.getMessage());
        }
        return null;
    }

    @Override
    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error(">>> [UserDAO] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean save(User user) {
        String sql = "INSERT INTO users (id, username, password, email, full_name, phone, address, " +
                     "is_active, role, balance, store_name, rating) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
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
            logger.error(">>> [UserDAO] Lỗi save: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error(">>> [UserDAO] Lỗi existsByUsername: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error(">>> [UserDAO] Lỗi findAll: " + e.getMessage());
        }
        return users;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone = ?, address = ?, " +
                     "is_active = ?, role = ?, balance = ?, store_name = ?, rating = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1,  user.getEmail());
            stmt.setString(2,  user.getFullName());
            stmt.setString(3,  user.getPhone());
            stmt.setString(4,  user.getAddress());
            stmt.setBoolean(5, user.isActive());
            stmt.setString(6,  user.getRole().name());
            stmt.setDouble(7,  user.getBalance());
            stmt.setString(8,  user.getStoreName());
            stmt.setDouble(9,  user.getRating());
            stmt.setString(10, user.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error(">>> [UserDAO] Lỗi update: " + e.getMessage());
        }
        return false;
    }

    @Override
    public User findFirstByRole(com.auction.model.entity.UserRole role) {
        String sql = "SELECT * FROM users WHERE role = ? AND is_active = TRUE LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error(">>> [UserDAO] Lỗi findFirstByRole: " + e.getMessage());
        }
        return null;
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
