package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SINGLETON PATTERN — Đảm bảo toàn bộ Server chỉ dùng 1 kết nối DB duy nhất.
 *
 * Tại sao Singleton?
 *   - 20 ClientHandler cùng chạy → nếu mỗi handler tạo riêng 1 Connection → 20 kết nối → MySQL quá tải
 *   - Singleton đảm bảo: dù 100 handler, tất cả dùng chung 1 bộ quản lý kết nối
 *
 * Cách dùng:
 *   Connection conn = DatabaseConnection.getInstance().getConnection();
 *   // KHÔNG BAO GIỜ gọi: new DatabaseConnection()
 */
public class DatabaseConnection {

    // 1. Biến static giữ instance duy nhất (ban đầu = null)
    private static DatabaseConnection instance;

    // 2. Thông tin kết nối MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection connection;

    // 3. Constructor PRIVATE → không ai bên ngoài tạo được
    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(">>> [DB] Kết nối MySQL thành công!");
        } catch (ClassNotFoundException e) {
            System.out.println(">>> [DB] LỖI: Không tìm thấy MySQL Driver!");
        } catch (SQLException e) {
            System.out.println(">>> [DB] LỖI kết nối: " + e.getMessage());
        }
    }

    // 4. Method static duy nhất để lấy instance — synchronized để thread-safe
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // 5. Lấy Connection để dùng trong DAO
    public Connection getConnection() {
        try {
            // Kiểm tra connection còn sống không, nếu chết thì tạo lại
            if (connection == null || connection.isClosed()) {
                instance = new DatabaseConnection();
            }
        } catch (SQLException e) {
            System.out.println(">>> [DB] LỖI kiểm tra connection: " + e.getMessage());
        }
        return connection;
    }
}
