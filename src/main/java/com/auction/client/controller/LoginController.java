package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller quản lý chức năng Đăng nhập và Đăng ký phía Client.
 */
public class LoginController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    // CÁC THÀNH PHẦN GIAO DIỆN CHÍNH & TAB ĐIỀU HƯỚNG FXML
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Button tabLogin;
    @FXML private Button tabRegister;
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;

    // CÁC TRƯỜNG NHẬP LIỆU GIAO DIỆN ĐĂNG NHẬP FXML
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginMessage;

    // CÁC TRƯỜNG NHẬP LIỆU GIAO DIỆN ĐĂNG KÝ FXML
    @FXML private TextField regFullName;
    @FXML private TextField regEmail;
    @FXML private TextField regUsername;
    @FXML private TextField regPhone;
    @FXML private TextField regAddress;
    @FXML private TextField regStoreName;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label regMessage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        switchToLoginTab();
    }

    /**
     * Chuyển đổi giao diện sang Tab Đăng nhập.
     */
    @FXML
    public void switchToLoginTab() {
        tabLogin.getStyleClass().removeAll("auth-tab");
        tabLogin.getStyleClass().add("auth-tab-active");
        tabRegister.getStyleClass().removeAll("auth-tab-active");
        tabRegister.getStyleClass().add("auth-tab");

        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);

        lblTitle.setText("Chào mừng trở lại");
        lblSubtitle.setText("Đăng nhập để tham gia đấu giá ngay hôm nay");
    }

    /**
     * Chuyển đổi giao diện sang Tab Đăng ký.
     */
    @FXML
    public void switchToRegisterTab() {
        tabRegister.getStyleClass().removeAll("auth-tab");
        tabRegister.getStyleClass().add("auth-tab-active");
        tabLogin.getStyleClass().removeAll("auth-tab-active");
        tabLogin.getStyleClass().add("auth-tab");

        registerForm.setVisible(true);
        registerForm.setManaged(true);
        loginForm.setVisible(false);
        loginForm.setManaged(false);

        lblTitle.setText("Tạo tài khoản mới");
        lblSubtitle.setText("Gia nhập cộng đồng đấu giá lớn nhất UET");
    }

    /**
     * Xử lý Đăng nhập.
     */
    @FXML
    public void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            loginMessage.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        loginMessage.setText("Đang đăng nhập...");

        ClientSocketManager.getInstance().execute(() -> {
            LoginDTO dto = new LoginDTO(username, password);
            Request req = new Request(RequestType.LOGIN, dto);
            try {
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                        UserResponseDTO user = res.getPayloadAs(UserResponseDTO.class);
                        SessionManager.getInstance().setCurrentUser(user);
                        navigateToMain();
                    } else {
                        String errorMsg = (res != null) ? res.getMessage() : "Lỗi kết nối server!";
                        loginMessage.setText(errorMsg);
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Xảy ra ngoại lệ ngắt kết nối mạng khi đang thực thi tiến trình Đăng nhập", e);
                Platform.runLater(() -> loginMessage.setText("Lỗi kết nối server!"));
            }
        });
    }

    /**
     * Xử lý Đăng ký.
     */
    @FXML
    public void handleRegister() {
        String fullName = regFullName.getText().trim();
        String email = regEmail.getText().trim();
        String username = regUsername.getText().trim();
        String password = regPassword.getText().trim();
        String confirm = regConfirm.getText().trim();

        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            regMessage.setText("Vui lòng nhập các trường có dấu *");
            return;
        }

        if (!password.equals(confirm)) {
            regMessage.setText("Mật khẩu xác nhận không khớp");
            return;
        }

        regMessage.setText("Đang xử lý đăng ký...");

        ClientSocketManager.getInstance().execute(() -> {
            RegisterDTO dto = new RegisterDTO(
                    username, password, email, fullName,
                    regPhone.getText().trim(), regAddress.getText().trim(),
                    regStoreName.getText().trim()
            );
            Request req = new Request(RequestType.REGISTER, dto);
            try {
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
                        switchToLoginTab();
                    } else {
                        String errorMsg = (res != null) ? res.getMessage() : "Lỗi kết nối server!";
                        regMessage.setText(errorMsg);
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Xảy ra ngoại lệ ngắt kết nối mạng khi đang thực thi tiến trình Đăng ký", e);
                Platform.runLater(() -> regMessage.setText("Lỗi kết nối server!"));
            }
        });
    }

    /**
     * Định tuyến vào trang chính hệ thống.
     */
    private void navigateToMain() {
        try {
            URL resource = getClass().getResource("/Main.fxml");
            if (resource == null) {
                throw new IOException("Không tìm thấy tệp tài nguyên thiết kế giao diện /Main.fxml");
            }
            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) loginForm.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.error("Hệ thống gặp lỗi nghiêm trọng trong tiến trình nạp tài nguyên tệp tin giao diện /Main.fxml", e);
        }
    }
}