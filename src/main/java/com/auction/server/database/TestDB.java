package com.auction.server.database;

import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Đang thử kết nối tới Supabase...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                System.out.println("✅ KẾT NỐI THÀNH CÔNG TỚI SUPABASE!");
            } else {
                System.out.println("❌ Kết nối thất bại (conn = null).");
            }
        } catch (Exception e) {
            System.out.println("❌ LỖI KẾT NỐI:");
            e.printStackTrace();
        }
    }
}
