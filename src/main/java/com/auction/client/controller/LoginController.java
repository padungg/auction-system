package com.auction.client.controller;

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

        // 1. Gọi sang lớp Network để kiểm tra (dùng server protocol)
        UserResponseDTO user = checkLoginFromNetwork(username, password);

        // 2. Xử lý kết quả trả về
        if (user != null) {
            lblError.setVisible(false);
            // Lưu thông tin user vào session
            SessionManager.getInstance().setCurrentUser(user);
            // Phân quyền bằng UserRole enum từ server
            String role = (user.getRole() == UserRole.ADMIN) ? "ADMIN" : "USER";
            navigateToDashboard(role);
        } else {
            // Sai tài khoản hoặc mật khẩu
            lblError.setVisible(true);
        }
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
            lblError.setText("LỖI: SERVER CHƯA BẬT!");
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
        System.out.println("Chuyển sang màn hình đăng ký...");
        // Tương tự: switchScene("/register.fxml");
    }
}