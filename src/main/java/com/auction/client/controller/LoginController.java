package com.auction.client.controller;
import javafx.application.Platform;
import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.UserRole;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.Gson;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblError;

    @FXML
    private Label lblRegister;

    @FXML
    public void initialize() {
        // Đã sửa lỗi Null từ fxml của bạn
        if (lblRegister != null) {
            lblRegister.setOnMouseClicked(e -> handleRegister());
        }
        txtPassword.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("⚠️ Vui lòng nhập đầy đủ thông tin!");
            lblError.setVisible(true);
            return;
        }

        // Hiện thông báo đang xử lý để UX tốt hơn
        lblError.setText("⏳ Đang kết nối đến server...");
        lblError.setStyle("-fx-text-fill: #2563eb;"); // Đổi màu xanh
        lblError.setVisible(true);

        // ĐẨY TÁC VỤ GỌI MẠNG SANG LUỒNG NGẦM (BACKGROUND THREAD)
        new Thread(() -> {
            // 1. Gọi sang lớp Network để kiểm tra (Chạy ngầm không làm đơ UI)
            UserResponseDTO user = checkLoginFromNetwork(username, password);

            // 2. Dùng Platform.runLater để cập nhật lại Giao diện (Luồng UI)
            Platform.runLater(() -> {
                if (user != null) {
                    lblError.setVisible(false);
                    // Lưu thông tin user vào session
                    SessionManager.getInstance().setCurrentUser(user);
                    // Phân quyền bằng UserRole enum từ server
                    String role = (user.getRole() == UserRole.ADMIN) ? "ADMIN" : "USER";
                    navigateToDashboard(role);
                } else {
                    // Sai tài khoản hoặc mật khẩu
                    lblError.setText("⚠️ Tài khoản hoặc mật khẩu không chính xác!");
                    lblError.setStyle("-fx-text-fill: #ef4444;"); // Trả lại màu đỏ
                    lblError.setVisible(true);
                }
            });
        }).start();
    }

    /**
     * Gửi LoginDTO lên Server qua ClientSocketManager.
     * - Đóng gói LoginDTO vào Request(LOGIN, loginDTO)
     * - Nhận Response → kiểm tra status
     * - Cast payload → UserResponseDTO
     */
    private UserResponseDTO checkLoginFromNetwork(String username, String password) {
        try {
            ClientSocketManager manager = ClientSocketManager.getInstance();

            // Kết nối nếu chưa kết nối
            if (!manager.isConnected()) {
                manager.connect("localhost", 8080);
            }

            // Tạo DTO và Request theo format server
            LoginDTO loginDTO = new LoginDTO(username, password);
            Request request = new Request(RequestType.LOGIN, loginDTO);

            // Gửi request, nhận response
            Response response = manager.sendRequest(request);

            // Kiểm tra status từ server
            if (response.getStatus() == ResponseStatus.SUCCESS) {
                // Cast payload → UserResponseDTO
                Gson gson = manager.getGson();
                String payloadJson = gson.toJson(response.getPayload());
                return gson.fromJson(payloadJson, UserResponseDTO.class);
            } else {
                // Server trả về lỗi → hiển thị message
                System.err.println("[Login] Server trả về: " + response.getMessage());
                return null;
            }

        } catch (IOException e) {
            System.err.println("[Login] Lỗi kết nối: " + e.getMessage());
            // BUG FIX: phải dùng Platform.runLater khi cập nhật UI từ background thread
            Platform.runLater(() -> {
                lblError.setText("❌ Không thể kết nối đến server! Vui lòng kiểm tra lại.");
                lblError.setStyle("-fx-text-fill: #ef4444;");
                lblError.setVisible(true);
            });
            return null;
        }
    }

    private void navigateToDashboard(String role) {
        try {
            // Chọn file FXML dựa trên quyền
            String fxmlFile = role.equals("ADMIN") ? "/admin.fxml" : "/view.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen(); // Căn giữa màn hình cho đẹp
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi load file FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi load register.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
