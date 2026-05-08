package com.auction.server.database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void initialize() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "username VARCHAR(50) UNIQUE NOT NULL, " +
                "password VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100), " +
                "full_name VARCHAR(100), " +
                "phone VARCHAR(20), " +
                "address VARCHAR(255), " +
                "is_active BOOLEAN, " +
                "role VARCHAR(20), " +
                "balance DOUBLE, " +
                "store_name VARCHAR(100), " +
                "rating DOUBLE" +
                ");";

        String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "condition_item VARCHAR(50), " +
                "seller_id VARCHAR(50), " +
                "starting_price DOUBLE, " +
                "item_type VARCHAR(20), " +
                "artist_name VARCHAR(100), " +
                "material VARCHAR(100), " +
                "creation_year INT, " +
                "brand VARCHAR(100), " +
                "model VARCHAR(100), " +
                "year INT, " +
                "km INT, " +
                "warranty_months INT, " +
                "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE" +
                ");";

        String createAuctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "item_id VARCHAR(50), " +
                "current_winner_id VARCHAR(50), " +
                "current_price DOUBLE, " +
                "start_time TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "status VARCHAR(20), " +
                "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE" +
                ");";

        String createBidTransactionsTable = "CREATE TABLE IF NOT EXISTS bid_transactions (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "bidder_id VARCHAR(50), " +
                "auction_id VARCHAR(50), " +
                "bid_amount DOUBLE, " +
                "bid_time TIMESTAMP, " +
                "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE" +
                ");";

        String createAutoBidsTable = "CREATE TABLE IF NOT EXISTS auto_bids (" +
                "user_id VARCHAR(50), " +
                "auction_id VARCHAR(50), " +
                "max_bid DOUBLE, " +
                "increment DOUBLE, " +
                "registered_at TIMESTAMP, " +
                "PRIMARY KEY (user_id, auction_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE" +
                ");";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
             
            stmt.execute(createUsersTable);
            stmt.execute(createItemsTable);
            stmt.execute(createAuctionsTable);
            stmt.execute(createBidTransactionsTable);
            stmt.execute(createAutoBidsTable);
            System.out.println(">>> [DB] Kiểm tra và khởi tạo các bảng thành công!");

        } catch (SQLException e) {
            System.err.println(">>> [DB] LỖI khởi tạo bảng: " + e.getMessage());
        }
    }
}
