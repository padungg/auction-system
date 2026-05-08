package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cung cấp kết nối đến MySQL.
 * Khởi tạo một kết nối mới mỗi khi được gọi để đảm bảo an toàn cho đa luồng
 * và tương thích với cú pháp try-with-resources trong các lớp DAO.
 */
public class DatabaseConnection {

    // Thông tin kết nối MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/auction?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "Dung2007@";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println(">>> [DB] LỖI: Không tìm thấy MySQL Driver!");
        }
    }

    /**
     * Trả về một Connection mới mỗi lần gọi.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        return conn;
    }
}
