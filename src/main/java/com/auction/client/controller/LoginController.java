package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Controller chịu trách nhiệm kiểm soát luồng xác thực hệ thống, bao gồm Đăng nhập (Login) và Đăng ký (Register).
 * Quản lý tính toàn vẹn của dữ liệu đầu vào trên các biểu mẫu và đồng bộ hóa trạng thái hiển thị của giao diện người dùng.
 */
public class LoginController implements Initializable {

    /**
     * Khởi tạo thành phần Logger hệ thống.
     * Thư viện này cung cấp cơ chế giám sát tập trung, hỗ trợ ghi vết chi tiết cấu trúc lỗi (Stack trace)
     * và phân loại mức độ nghiêm trọng giúp việc rà soát lỗi mạng trở nên hiệu quả hơn khi ứng dụng vận hành.
     */
    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    private final Gson GSON = GsonProvider.getInstance();

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - KHUNG ĐIỀU HƯỚNG TỔNG (CONTAINERS & TABS)
    // =========================================================================
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Button tabLogin;
    @FXML private Button tabRegister;
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - FORM ĐĂNG NHẬP (LOGIN FIELDS)
    // =========================================================================
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginMessage;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - FORM ĐĂNG KÝ (REGISTER FIELDS)
    // =========================================================================
    @FXML private TextField regFullName;
    @FXML private TextField regEmail;
    @FXML private TextField regUsername;
    @FXML private TextField regPhone;
    @FXML private TextField regAddress;
    @FXML private TextField regStoreName;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label regMessage;

    /**
     * Phương thức khởi tạo chu kỳ hiển thị giao diện JavaFX (Lifecycle Hook).
     * Thiết lập mặc định tập trung hiển thị biểu mẫu Đăng nhập ngay khi màn hình xác thực được tải lên.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        switchToLoginTab(null);
    }

    /**
     * Chuyển đổi trạng thái hiển thị của giao diện sang biểu mẫu Đăng nhập.
     * Thực hiện cập nhật các tập lệnh CSS Style và đóng/mở quản lý các Layout Container tương ứng.
     */
    @FXML
    public void switchToLoginTab(Event event) {
        tabLogin.getStyleClass().removeAll("auth-tab");
        tabLogin.getStyleClass().add("auth-tab-active");
        tabRegister.getStyleClass().removeAll("auth-tab-active");
        tabRegister.getStyleClass().add("auth-tab");

        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);

        // Chuẩn hóa văn bản hiển thị giao diện thuần sạch sẽ để bảo vệ font chữ hệ thống
        lblTitle.setText("Chào mừng trở lại");
        lblSubtitle.setText("Đăng nhập để tham gia đấu giá ngay hôm nay");
    }

    /**
     * Chuyển đổi trạng thái hiển thị của giao diện sang biểu mẫu Đăng ký thành viên mới.
     * Thực hiện cập nhật các tập lệnh CSS Style và đóng/mở quản lý các Layout Container tương ứng.
     */
    @FXML
    public void switchToRegisterTab(Event event) {
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
     * Tiếp nhận và xử lý sự kiện yêu cầu Đăng nhập tài khoản của người dùng.
     * Kiểm tra tính hợp lệ của dữ liệu biểu mẫu và khởi chạy luồng bất đồng bộ để truyền tải gói tin mạng tới Server.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            loginMessage.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        loginMessage.setText("Đang đăng nhập...");

        // Cô lập tác vụ I/O mạng trên tiểu trình nền tách biệt nhằm bảo vệ luồng tương tác chính (UI Thread) khỏi hiện tượng nghẽn mạch
        new Thread(() -> {
            LoginDTO dto = new LoginDTO(username, password);
            Request req = new Request(RequestType.LOGIN, dto);
            try {
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                Platform.runLater(() -> {
                    if (res.getStatus() == ResponseStatus.SUCCESS) {
                        UserResponseDTO user = GSON.fromJson(GSON.toJson(res.getPayload()), UserResponseDTO.class);
                        SessionManager.getInstance().setCurrentUser(user);
                        navigateToMain();
                    } else {
                        loginMessage.setText(res.getMessage());
                    }
                });
            } catch (IOException e) {
                // Đăng ký vết lỗi ngoại lệ I/O Socket mạng vào bộ giám sát Logger chuyên dụng với mức độ nghiêm trọng SEVERE
                logger.log(Level.SEVERE, "Xảy ra ngoại lệ ngắt kết nối mạng khi đang thực thi tiến trình Đăng nhập: ", e);
                Platform.runLater(() -> loginMessage.setText("Lỗi kết nối server!"));
            }
        }).start();
    }

    /**
     * Tiếp nhận và xử lý sự kiện yêu cầu Đăng ký thành viên mới của người dùng.
     * Đánh giá tính toàn vẹn của các trường dữ liệu bắt buộc, so khớp mật khẩu và chuyển tiếp cấu trúc DTO lên máy chủ.
     */
    @FXML
    public void handleRegister(ActionEvent event) {
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

        // Phân phối luồng mạng xử lý độc lập để duy trì tính mượt mà cho trải nghiệm tương tác lớp giao diện
        new Thread(() -> {
            RegisterDTO dto = new RegisterDTO(
                    username, password, email, fullName,
                    regPhone.getText().trim(), regAddress.getText().trim(),
                    regStoreName.getText().trim()
            );
            Request req = new Request(RequestType.REGISTER, dto);
            try {
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                Platform.runLater(() -> {
                    if (res.getStatus() == ResponseStatus.SUCCESS) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setHeaderText(null);
                        alert.setContentText("Đăng ký thành công! Vui lòng đăng nhập.");
                        alert.showAndWait();
                        switchToLoginTab(null);
                    } else {
                        regMessage.setText(res.getMessage());
                    }
                });
            } catch (IOException e) {
                // Đồng bộ hóa cấu trúc lưu vết sự cố kết nối Socket thông qua Logger hệ thống
                logger.log(Level.SEVERE, "Xảy ra ngoại lệ ngắt kết nối mạng khi đang thực thi tiến trình Đăng ký: ", e);
                Platform.runLater(() -> regMessage.setText("Lỗi kết nối server!"));
            }
        }).start();
    }

    /**
     * Thay đổi cấu trúc Scene hiện hành để chuyển tiếp luồng làm việc của người dùng vào phân hệ màn hình ứng dụng chính (Main UI).
     */
    private void navigateToMain() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            Stage stage = (Stage) loginForm.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (IOException e) {
            // Đăng ký thông báo lỗi nghiêm trọng khi cấu trúc cây tài nguyên tệp tin fxml không thể khởi tạo thành công
            logger.log(Level.SEVERE, "Hệ thống gặp lỗi nghiêm trọng trong tiến trình nạp tài nguyên tệp tin giao diện /main.fxml: ", e);
        }
    }
}