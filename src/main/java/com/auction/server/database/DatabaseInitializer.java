package com.auction.server.database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseInitializer {

        public static void initialize() {

                // ══════════════════════════════════════════════════════════════
                // 1. TẠO BẢNG (CREATE TABLE IF NOT EXISTS)
                // ══════════════════════════════════════════════════════════════

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
                                "is_auto_bid BOOLEAN DEFAULT FALSE, " +
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

                        // ── Tạo bảng ──────────────────────────────────────────
                        stmt.execute(createUsersTable);
                        stmt.execute(createItemsTable);
                        stmt.execute(createAuctionsTable);
                        stmt.execute(createBidTransactionsTable);
                        stmt.execute(createAutoBidsTable);

                        // Tự động thêm cột is_auto_bid nếu database cũ chưa có (tránh lỗi khi người
                        // dùng quên xóa DB)
                        try {
                                stmt.execute("ALTER TABLE bid_transactions ADD COLUMN is_auto_bid BOOLEAN DEFAULT FALSE;");
                        } catch (SQLException ignored) {
                                // Cột đã tồn tại hoặc bảng chưa có data, có thể bỏ qua
                        }

                        // ══════════════════════════════════════════════════════
                        // 2. SEED TÀI KHOẢN
                        // ══════════════════════════════════════════════════════

                        stmt.execute("INSERT IGNORE INTO users (id, username, password, email, full_name, phone, address, is_active, role, balance, store_name, rating) "
                                        +
                                        "VALUES ('USR-ADMIN-001', 'admin', '123', 'admin@auction.com', 'Administrator', '0123456789', 'Hanoi', true, 'ADMIN', 10000000.0, 'Admin Store', 5.0);");

                        stmt.execute("INSERT IGNORE INTO users (id, username, password, email, full_name, phone, address, is_active, role, balance, store_name, rating) "
                                        +
                                        "VALUES ('USR-MEM-001', 'member', '123', 'member@auction.com', 'Member 1', '0987654321', 'HCM', true, 'MEMBER', 5000000.0, NULL, 5.0);");

                        // ══════════════════════════════════════════════════════
                        // 3. SEED SẢN PHẨM (7 item cho 7 phiên đấu giá)
                        // ══════════════════════════════════════════════════════

                        // -- 3 item gốc (tương ứng AUC-001, AUC-002, AUC-003 tạo thủ công) --
                        stmt.execute("INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) "
                                        +
                                        "VALUES ('ITEM-ART-001', 'Tranh Đông Hồ', 'Tranh dân gian Đông Hồ chính gốc', 'Mới', 'USR-ADMIN-001', 500000.0, 'ART', 'Nghệ nhân Đông Hồ', 'Giấy điệp', 2023, NULL, NULL, NULL, NULL, NULL);");

                        stmt.execute("INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) "
                                        +
                                        "VALUES ('ITEM-ELE-001', 'Tủ lạnh Samsung', 'Tủ lạnh Samsung Inverter 2 cánh', 'Mới', 'USR-ADMIN-001', 4000000.0, 'ELECTRONICS', NULL, NULL, NULL, 'Samsung', NULL, NULL, NULL, 24);");

                        stmt.execute("INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) "
                                        +
                                        "VALUES ('ITEM-VEH-001', 'VF3', 'Xe ô tô điện VinFast VF3 siêu tiết kiệm', 'Mới', 'USR-ADMIN-001', 240000000.0, 'VEHICLE', NULL, NULL, NULL, 'VinFast', 'VF3', 2024, 0, NULL);");

                        // -- 4 item bổ sung (tương ứng AUC-010, AUC-011, AUC-012, AUC-013) --
                        stmt.execute("INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) "
                                        +
                                        "VALUES ('ITEM-ELE-010', 'MacBook Pro M2 2023', 'Sản phẩm mới 100%, nguyên seal. Hiệu năng vượt trội.', 'Mới', 'USR-ADMIN-001', 30000000.0, 'ELECTRONICS', NULL, NULL, NULL, 'Apple', NULL, NULL, NULL, 12);");

                        stmt.execute("INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) "
                                        +
                                        "VALUES ('ITEM-ART-010', 'Tranh Sơn Mài Hồ Gươm', 'Tranh sơn mài thủ công, phong cảnh Hồ Gươm.', 'Mới', 'USR-ADMIN-001', 5000000.0, 'ART', 'Nguyễn Văn Tài', 'Sơn mài', 2022, NULL, NULL, NULL, NULL, NULL);");

                        stmt.execute("INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) "
                                        +
                                        "VALUES ('ITEM-VEH-010', 'Honda Air Blade 2022', 'Xe nguyên bản, đã đi 12.000km.', 'Như mới', 'USR-ADMIN-001', 28000000.0, 'VEHICLE', NULL, NULL, NULL, 'Honda', 'Air Blade 125', 2022, 12000, NULL);");

                        stmt.execute("INSERT IGNORE INTO items (id, name, description, condition_item, seller_id, starting_price, item_type, artist_name, material, creation_year, brand, model, year, km, warranty_months) "
                                        +
                                        "VALUES ('ITEM-ELE-011', 'iPhone 15 Pro Max 256GB', 'Hàng chính hãng VN/A, máy như mới 99%.', 'Như mới', 'USR-ADMIN-001', 22000000.0, 'ELECTRONICS', NULL, NULL, NULL, 'Apple', NULL, NULL, NULL, 9);");

                        // ══════════════════════════════════════════════════════
                        // 4. SEED PHIÊN ĐẤU GIÁ (7 phiên)
                        // ══════════════════════════════════════════════════════

                        // -- 3 phiên gốc (khớp với data đã tạo thủ công) --
                        stmt.execute("INSERT IGNORE INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                                        +
                                        "VALUES ('AUC-001', 'ITEM-ART-001', NULL, 8700000.0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'FINISHED');");

                        stmt.execute("INSERT IGNORE INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                                        +
                                        "VALUES ('AUC-002', 'ITEM-ELE-001', NULL, 35500000.0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), 'RUNNING');");

                        stmt.execute("INSERT IGNORE INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                                        +
                                        "VALUES ('AUC-003', 'ITEM-VEH-001', NULL, 33000000.0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 'RUNNING');");

                        // -- 4 phiên bổ sung --
                        stmt.execute("INSERT IGNORE INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                                        +
                                        "VALUES ('AUC-010', 'ITEM-ELE-010', NULL, 30000000.0, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'RUNNING');");

                        stmt.execute("INSERT IGNORE INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                                        +
                                        "VALUES ('AUC-011', 'ITEM-ART-010', NULL, 5000000.0, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'OPEN');");

                        stmt.execute("INSERT IGNORE INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                                        +
                                        "VALUES ('AUC-012', 'ITEM-VEH-010', 'USR-MEM-001', 32000000.0, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'FINISHED');");

                        stmt.execute("INSERT IGNORE INTO auctions (id, item_id, current_winner_id, current_price, start_time, end_time, status) "
                                        +
                                        "VALUES ('AUC-013', 'ITEM-ELE-011', 'USR-MEM-001', 25000000.0, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'PAID');");

                        // ══════════════════════════════════════════════════════
                        // 5. SEED BID MẪU (cho các phiên đã kết thúc)
                        // ══════════════════════════════════════════════════════

                        stmt.execute("INSERT IGNORE INTO bid_transactions (id, bidder_id, auction_id, bid_amount, bid_time, is_auto_bid) "
                                        +
                                        "VALUES ('BID-001', 'USR-MEM-001', 'AUC-012', 32000000.0, DATE_SUB(NOW(), INTERVAL 2 DAY), FALSE);");

                        stmt.execute("INSERT IGNORE INTO bid_transactions (id, bidder_id, auction_id, bid_amount, bid_time, is_auto_bid) "
                                        +
                                        "VALUES ('BID-002', 'USR-MEM-001', 'AUC-013', 25000000.0, DATE_SUB(NOW(), INTERVAL 6 DAY), FALSE);");

                        System.out.println(
                                        ">>> [DB] Khởi tạo thành công! Tổng 7 phiên đấu giá đã được seed vào auction_system.");

                } catch (SQLException e) {
                        System.err.println(">>> [DB] LỖI khởi tạo bảng: " + e.getMessage());
                }
        }
}
