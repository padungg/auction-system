package com.auction.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cung cấp kết nối đến MySQL.
 * Khởi tạo một kết nối mới mỗi khi được gọi để đảm bảo an toàn cho đa luồng
 * và tương thích với cú pháp try-with-resources trong các lớp DAO. Nhatdogy2007
 */
public class DatabaseConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);

    // Thông tin kết nối MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/auction_system?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "Dung2007@";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.error(">>> [DB] LỖI: Không tìm thấy MySQL Driver!");
        }
    }

    /**
     * Trả về một Connection mới mỗi lần gọi.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
