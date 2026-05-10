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

        String insertAdmin = "INSERT IGNORE INTO users (id, username, password, email, full_name, phone, address, is_active, role, balance, store_name, rating) " +
                "VALUES ('USR-ADMIN-001', 'admin', '123', 'admin@auction.com', 'Administrator', '0123456789', 'Hanoi', true, 'ADMIN', 10000000.0, 'Admin Store', 5.0);";

        String insertMember = "INSERT IGNORE INTO users (id, username, password, email, full_name, phone, address, is_active, role, balance, store_name, rating) " +
                "VALUES ('USR-MEM-001', 'member', '123', 'member@auction.com', 'Member 1', '0987654321', 'HCM', true, 'MEMBER', 5000000.0, NULL, 5.0);";

        String insertArt = "INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) " +
                "VALUES ('ITEM-ART-001', 'Tranh Đông Hồ', 'Tranh dân gian Đông Hồ chính gốc', 'Mới', 'USR-ADMIN-001', 500000.0, 'ART', 'Nghệ nhân Đông Hồ', 'Giấy điệp', 2023, NULL, NULL, NULL, NULL, NULL);";

        String insertElectronic = "INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) " +
                "VALUES ('ITEM-ELE-001', 'Tủ lạnh', 'Tủ lạnh Samsung Inverter', 'Mới', 'USR-ADMIN-001', 4000000.0, 'ELECTRONICS', NULL, NULL, NULL, 'Samsung', NULL, NULL, NULL, 24);";

        String insertVehicle = "INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) " +
                "VALUES ('ITEM-VEH-001', 'VF3', 'Xe ô tô điện VinFast VF3 siêu tiết kiệm', 'Mới', 'USR-ADMIN-001', 240000000.0, 'VEHICLE', NULL, NULL, NULL, 'VinFast', 'VF3', 2024, 0, NULL);";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
             
            stmt.execute(createUsersTable);
            stmt.execute(createItemsTable);
            stmt.execute(createAuctionsTable);
            stmt.execute(createBidTransactionsTable);
            stmt.execute(createAutoBidsTable);
            
            // Chèn dữ liệu mẫu
            stmt.execute(insertAdmin);
            stmt.execute(insertMember);
            
            // Chèn dữ liệu sản phẩm mẫu
            stmt.execute(insertArt);
            stmt.execute(insertElectronic);
            stmt.execute(insertVehicle);
            
            System.out.println(">>> [DB] Kiểm tra và khởi tạo các bảng thành công!");

        } catch (SQLException e) {
            System.err.println(">>> [DB] LỖI khởi tạo bảng: " + e.getMessage());
        }
    }
}
