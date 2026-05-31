package com.auction.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cung cấp kết nối đến PostgreSQL (Supabase) thông qua HikariCP.
 * Quản lý bể kết nối (Connection Pool) giúp tối ưu hiệu suất và tránh cạn kiệt tài nguyên.
 */
public class DatabaseConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);

    private static HikariDataSource dataSource;

    // Thông tin kết nối PostgreSQL (Supabase) qua cổng Pooler (6543)
    private static final String URL = "jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:6543/postgres?options=-c%20timezone=Asia/Ho_Chi_Minh";
    private static final String USER = "postgres.ygigpkapniuhjtpetcta";
    private static final String PASSWORD = "h4B-DbpEu/my&4W";

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setDriverClassName("org.postgresql.Driver");

            // Tối ưu hóa HikariCP cho Supabase
            config.setMaximumPoolSize(15);
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000);
            config.setConnectionTimeout(10000); // Chờ tối đa 10s
            config.setMaxLifetime(1800000); // 30 phút

            dataSource = new HikariDataSource(config);
            LOGGER.info(">>> [DB] Khởi tạo thành công HikariCP Connection Pool kết nối tới Supabase!");
        } catch (Exception e) {
            LOGGER.error(">>> [DB] LỖI: Không thể khởi tạo HikariCP: {}", e.getMessage(), e);
        }
    }

    /**
     * Lấy Connection từ bể kết nối (Pool).
     * Khi gọi conn.close() trong try-with-resources ở DAO, connection sẽ tự động được trả lại bể.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("HikariDataSource chưa được khởi tạo!");
        }
        return dataSource.getConnection();
    }
}
