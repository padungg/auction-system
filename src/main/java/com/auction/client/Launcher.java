package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điểm khởi chạy chính của ứng dụng client JavaFX.
 * Thực hiện kết nối socket, nạp giao diện đăng nhập và quản lý vòng đời ứng dụng.
 */
public class Launcher extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    /**
     * Khởi tạo giao diện chính của JavaFX và kết nối đến Server.
     */
    @Override
    public void start(Stage primaryStage) {
        // Lấy thông tin cấu hình Server từ tham số JVM, mặc định là localhost:8080
        String host = System.getProperty("server.host", "localhost");
        int port = Integer.getInteger("server.port", 8080);

        try {
            // Thiết lập kết nối socket TCP tới Server
            com.auction.client.network.ClientSocketManager.getInstance().connect(host, port);

            // Nạp giao diện FXML cho màn hình đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("UET Auction System");
            primaryStage.setResizable(false);

            // Thiết lập icon cho ứng dụng (hình búa đấu giá màu xanh sáng)
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/gavel-light-blue.png")));
            } catch (Exception e) {
                LOGGER.warn("Không thể nạp icon búa đấu giá màu xanh sáng: {}", e.getMessage());
            }

            // Đăng ký sự kiện đóng cửa sổ để ngắt kết nối và giải phóng tài nguyên
            primaryStage.setOnCloseRequest(event -> {
                LOGGER.info("Đang đóng ứng dụng và giải phóng tài nguyên mạng...");
                com.auction.client.network.ClientSocketManager.getInstance().disconnect();
                System.exit(0);
            });

            primaryStage.show();

        } catch (Exception e) {
            LOGGER.error("Lỗi nghiêm trọng khi khởi chạy ứng dụng client tại {}:{}", host, port, e);

            // Hiển thị thông báo lỗi kết nối và tắt ứng dụng
            String errorMsg = String.format("Không thể kết nối tới Server tại %s:%d.\n" +
                    "Chi tiết: %s\n\nVui lòng kiểm tra lại trạng thái Server.", host, port, e.getMessage());

            com.auction.client.util.AlertUtils.showError("Lỗi Kết Nối", errorMsg);
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}