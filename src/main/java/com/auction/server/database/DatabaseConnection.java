package com.auction.server.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cung cấp kết nối đến PostgreSQL (Supabase).
 * Khởi tạo một kết nối mới mỗi khi được gọi để đảm bảo an toàn cho đa luồng
 * và tương thích với cú pháp try-with-resources trong các lớp DAO. Nhatdogy2007
 */
public class DatabaseConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);

    // Thông tin kết nối PostgreSQL (Supabase)
    private static final String URL = "jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:6543/postgres";
    private static final String USER = "postgres.ygigpkapniuhjtpetcta";
    private static final String PASSWORD = "h4B-DbpEu/my&4W";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.error(">>> [DB] LỖI: Không tìm thấy PostgreSQL Driver!");
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
