package com.auction.client.controller;

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

        // 1. Gọi sang lớp Network để kiểm tra (tạm thời giả lập)
        String role = checkLoginFromNetwork(username, password);

        // 2. Xử lý kết quả trả về
        if (role != null) {
            lblError.setVisible(false);
            navigateToDashboard(role);
        } else {
            // Sai tài khoản hoặc mật khẩu
            lblError.setVisible(true);
        }
    }

    /**
     * SAU NÀY: Bạn sẽ gọi ClientNetwork.sendLogin(user, pass) ở đây
     * Hiện tại: Trả về role giả lập để bạn test luồng chuyển cảnh
     */
    private String checkLoginFromNetwork(String username, String password) {
        if (password.equals("123")) {
            if (username.equals("admin")) return "ADMIN";
            return "USER"; // Mọi tên khác đều là USER
        }
        return null; // Đăng nhập thất bại
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