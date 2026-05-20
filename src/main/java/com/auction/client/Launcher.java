package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp khởi chạy ứng dụng (Application Launcher).
 * Đóng vai trò là điểm vào chính (Entry Point) của chương trình, chịu trách nhiệm thiết lập cổng kết nối Socket nền tảng,
 * nạp cấu hình giao diện đăng nhập ban đầu và quản lý tiến trình giải phóng tài nguyên mạng khi tắt ứng dụng.
 */
public class Launcher extends Application {

    /**
     * Khởi tạo hệ thống ghi nhật ký log theo tiêu chuẩn SLF4J nhằm giám sát chu kỳ khởi động ứng dụng JavaFX
     * và lưu vết các ngoại lệ nghiêm trọng liên quan đến tệp tin cấu hình FXML hoặc kết nối Socket ban đầu.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Thiết lập và khởi tạo cổng kết nối TCP đến máy chủ Server trước khi kết xuất giao diện người dùng UI
            com.auction.client.network.ClientSocketManager.getInstance().connect("localhost", 8080);

            // Kích hoạt nạp tệp tin cấu hình giao diện của phân hệ Đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("UET Auction System - Login");
            primaryStage.setResizable(false);

            // Đăng ký bộ lắng nghe sự kiện đóng cửa sổ để cưỡng chế ngắt kết nối Socket và giải phóng tài nguyên hệ thống
            primaryStage.setOnCloseRequest(event -> {
                com.auction.client.network.ClientSocketManager.getInstance().disconnect();
                System.exit(0);
            });

            primaryStage.show();
        } catch (Exception e) {
            LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình khởi chạy ứng dụng hệ thống Đấu giá", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}