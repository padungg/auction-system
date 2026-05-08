package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField txtFullName;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private Label lblError;

    @FXML
    public void initialize() {
        // Cho phép nhấn Enter ở ô confirm password để đăng ký
        txtConfirmPassword.setOnAction(e -> handleRegister());
    }

    @FXML
    private void handleRegister() {
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // Validate: kiểm tra trường rỗng
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("⚠️ Vui lòng điền đầy đủ tất cả các trường!");
            return;
        }

        // Validate: kiểm tra mật khẩu khớp
        if (!password.equals(confirmPassword)) {
            showError("⚠️ Mật khẩu không khớp! Vui lòng nhập lại.");
            return;
        }

        // Validate: mật khẩu tối thiểu 6 ký tự
        if (password.length() < 6) {
            showError("⚠️ Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        // Hiện thông báo đang xử lý
        lblError.setText("⏳ Đang tạo tài khoản...");
        lblError.setStyle("-fx-text-fill: #2563eb;");
        lblError.setVisible(true);

        // Gửi request trên background thread để không đơ UI
        new Thread(() -> {
            boolean success = sendRegisterRequest(fullName, username, password);

            Platform.runLater(() -> {
                if (success) {
                    // Đăng ký thành công → quay về màn hình Login
                    lblError.setText("✅ Đăng ký thành công! Đang chuyển sang đăng nhập...");
                    lblError.setStyle("-fx-text-fill: #16a34a;");
                    lblError.setVisible(true);

                    // Delay ngắn để user đọc thông báo, rồi chuyển màn hình
                    new Thread(() -> {
                        try {
                            Thread.sleep(1200);
                        } catch (InterruptedException ignored) {}
                        Platform.runLater(this::switchToLogin);
                    }).start();
                }
                // Nếu thất bại, lỗi đã được set trong sendRegisterRequest qua Platform.runLater
            });
        }).start();
    }

    /**
     * Gửi RegisterDTO lên server, trả về true nếu thành công.
     */
    private boolean sendRegisterRequest(String fullName, String username, String password) {
        try {
            ClientSocketManager manager = ClientSocketManager.getInstance();

            if (!manager.isConnected()) {
                manager.connect("localhost", 8080);
            }

            // email, phone, address để trống — server có thể cho phép null
            RegisterDTO dto = new RegisterDTO(username, password, "", fullName, "", "");
            Request request = new Request(RequestType.REGISTER, dto);
            Response response = manager.sendRequest(request);

            if (response.getStatus() == ResponseStatus.SUCCESS) {
                return true;
            } else {
                String msg = response.getMessage() != null
                        ? response.getMessage()
                        : "Tên đăng nhập đã tồn tại!";
                Platform.runLater(() -> showError("⚠️ " + msg));
                return false;
            }

        } catch (IOException e) {
            System.err.println("[Register] Lỗi kết nối: " + e.getMessage());
            Platform.runLater(() -> showError("❌ Không thể kết nối đến server! Vui lòng kiểm tra lại."));
            return false;
        }
    }

    @FXML
    private void switchToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi load Login.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setStyle("-fx-text-fill: #ef4444;");
        lblError.setVisible(true);
    }
}
