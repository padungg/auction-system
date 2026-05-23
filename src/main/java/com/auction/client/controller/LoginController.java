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
import javafx.event.ActionEvent;
import javafx.event.Event;
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
 * <h2>LoginController</h2>
 * <p>
 * Controller chịu trách nhiệm kiểm soát toàn bộ luồng xác thực hệ thống (Authentication Core)
 * bao gồm các nghiệp vụ Đăng nhập (Login) và Đăng ký thành viên mới (Register) trên ứng dụng Client.
 * </p>
 *
 * <p><b>Các cơ chế kỹ thuật tích hợp:</b></p>
 * <ul>
 *   <li><b>Kiểm soát giao diện Tab (Tab Toggle Engine):</b> Đồng bộ trạng thái hiển thị, hoán đổi động các container bố cục CSS (`loginForm`, `registerForm`) dựa trên hành động điều hướng.</li>
 *   <li><b>An toàn đa luồng (Multi-threading Safety):</b> Chuyển giao toàn bộ các tác vụ xử lý mạng IO (Network Requests) sang Worker Thread ngầm và sử dụng `Platform.runLater` để trả kết quả về UI Thread, ngăn chặn hiện tượng treo ứng dụng (UI Freezing).</li>
 *   <li><b>Quản lý phiên (Session Binding):</b> Đóng gói dữ liệu định danh nhận về từ Server và liên kết trực tiếp vào hệ thống quản lý bộ nhớ đệm tập trung `SessionManager`.</li>
 * </ul>
 *
 * @since 1.0
 * @see javafx.fxml.Initializable
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.client.util.SessionManager
 */
public class LoginController implements Initializable {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Lưu vết các hành vi xác thực, cô lập các sự cố ngắt kết nối Socket và giám sát luồng khởi tạo tài nguyên đồ họa.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

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
    // THÀNH PHẦN GIAO DIỆN FXML - BIỂU MẪU ĐĂNG NHẬP (LOGIN FORM FIELDS)
    // =========================================================================
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginMessage;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - BIỂU MẪU ĐĂNG KÝ (REGISTER FORM FIELDS)
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
     * Phương thức vòng đời khởi tạo JavaFX View (Lifecycle Hook).
     * Tự động thực thi sau khi kiến trúc tệp tin FXML được tải thành công.
     * Thiết lập trạng thái mặc định ban đầu là hiển thị phân hệ Đăng nhập.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        switchToLoginTab(null);
    }

    /**
     * Chuyển đổi giao diện sang Tab Đăng nhập (Switch to Login Mode).
     * Thực hiện làm mới danh sách lớp CSS định dạng để thay đổi đồ họa các nút tab,
     * đồng thời cấu hình ẩn hiện (Visible & Managed) các Layout Container tương ứng.
     *
     * @param event Sự kiện tương tác từ hệ thống JavaFX (Có thể truyền null nếu gọi nội bộ)
     */
    @FXML
    public void switchToLoginTab(Event event) {
        // Cập nhật cấu trúc lớp hiệu ứng phong cách CSS hoạt động cho Tab Đăng nhập
        tabLogin.getStyleClass().removeAll("auth-tab");
        tabLogin.getStyleClass().add("auth-tab-active");
        tabRegister.getStyleClass().removeAll("auth-tab-active");
        tabRegister.getStyleClass().add("auth-tab");

        // Quản lý trạng thái không gian phân bổ hiển thị của các Form biểu mẫu
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);

        // Đặt nội dung tiêu đề chuẩn hóa hướng dẫn người dùng
        lblTitle.setText("Chào mừng trở lại");
        lblSubtitle.setText("Đăng nhập để tham gia đấu giá ngay hôm nay");
    }

    /**
     * Chuyển đổi giao diện sang Tab Đăng ký thành viên mới (Switch to Register Mode).
     * Cập nhật các thuộc tính CSS nhận diện trạng thái và hoán đổi cấu trúc hiển thị biểu mẫu.
     *
     * @param event Sự kiện tương tác từ hệ thống JavaFX
     */
    @FXML
    public void switchToRegisterTab(Event event) {
        // Cập nhật cấu trúc lớp hiệu ứng phong cách CSS hoạt động cho Tab Đăng ký
        tabRegister.getStyleClass().removeAll("auth-tab");
        tabRegister.getStyleClass().add("auth-tab-active");
        tabLogin.getStyleClass().removeAll("auth-tab-active");
        tabLogin.getStyleClass().add("auth-tab");

        // Thay đổi kiến trúc phân bổ không gian Layout Container trên View
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        loginForm.setVisible(false);
        loginForm.setManaged(false);

        lblTitle.setText("Tạo tài khoản mới");
        lblSubtitle.setText("Gia nhập cộng đồng đấu giá lớn nhất UET");
    }

    /**
     * Đón nhận hành động xử lý Đăng nhập tài khoản (Login Action Handler).
     * Tiến hành xác thực nghiệp vụ cơ bản (Validation) tại chỗ, đóng gói cấu trúc LoginDTO
     * và chuyển giao mạng luồng ngầm để đối chiếu thông tin tài khoản mật khẩu từ Server.
     *
     * @param event Sự kiện kích hoạt Action từ nút bấm Đăng nhập FXML
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        // Kiểm tra ràng buộc dữ liệu đầu vào không được phép để trống
        if (username.isEmpty() || password.isEmpty()) {
            loginMessage.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        loginMessage.setText("Đang đăng nhập...");

        // Khởi chạy nhiệm vụ bất đồng bộ trên Worker Thread ngầm để tránh gây nghẽn luồng đồ họa chính
        ClientSocketManager.getInstance().execute(() -> {
            LoginDTO dto = new LoginDTO(username, password);
            Request req = new Request(RequestType.LOGIN, dto);
            try {
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                // Đồng bộ luồng phản hồi kết quả về JavaFX Application Thread an toàn
                Platform.runLater(() -> {
                    if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                        UserResponseDTO user = res.getPayloadAs(UserResponseDTO.class);
                        SessionManager.getInstance().setCurrentUser(user); // Gán thông tin người dùng vào Session Bộ nhớ đệm
                        navigateToMain(); // Kích hoạt điều hướng màn hình
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
     * Đón nhận hành động xử lý Đăng ký thành viên mới (Register Action Handler).
     * Xác thực các trường thuộc tính bắt buộc, đối sánh kiểm tra tính đồng nhất của mật khẩu,
     * đóng gói cấu trúc dữ liệu RegisterDTO và truyền dẫn mạng để khởi tạo tài khoản trên cơ sở dữ liệu.
     *
     * @param event Sự kiện kích hoạt Action từ nút bấm Đăng ký FXML
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        String fullName = regFullName.getText().trim();
        String email = regEmail.getText().trim();
        String username = regUsername.getText().trim();
        String password = regPassword.getText().trim();
        String confirm = regConfirm.getText().trim();

        // Kiểm tra tính toàn vẹn của tập dữ liệu có đính kèm ký hiệu bắt buộc (*)
        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            regMessage.setText("Vui lòng nhập các trường có dấu *");
            return;
        }

        // Kiểm tra logic khớp chuỗi mật khẩu xác nhận
        if (!password.equals(confirm)) {
            regMessage.setText("Mật khẩu xác nhận không khớp");
            return;
        }

        regMessage.setText("Đang xử lý đăng ký...");

        // Khởi chạy tác vụ IO kết nối mạng ngầm ngắt biệt độc lập với Main UI Thread
        ClientSocketManager.getInstance().execute(() -> {
            RegisterDTO dto = new RegisterDTO(
                    username, password, email, fullName,
                    regPhone.getText().trim(), regAddress.getText().trim(),
                    regStoreName.getText().trim()
            );
            Request req = new Request(RequestType.REGISTER, dto);
            try {
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                // Trả chuỗi điều khiển hiển thị kết quả về luồng đồ họa JavaFX chính
                Platform.runLater(() -> {
                    if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
                        switchToLoginTab(null); // Tự động quay lại giao diện tab đăng nhập ngay khi hoàn thành
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
     * Thực hiện thay đổi cấu trúc cây phân cấp Scene để định tuyến người dùng vào không gian làm việc chính.
     * Nạp tệp tin tài nguyên thiết kế giao diện `/Main.fxml` và cấu hình thuộc tính cửa sổ Stage.
     */
    private void navigateToMain() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Main.fxml"));
            Stage stage = (Stage) loginForm.getScene().getWindow(); // Trích xuất Stage hiện hành từ phần tử Node đồ họa
            stage.setScene(new Scene(root));
            stage.setResizable(true); // Bật quyền co giãn kích thước cửa sổ linh hoạt cho Dashboard chính
            stage.centerOnScreen(); // Đưa vị trí hiển thị cửa sổ về trung tâm màn hình thiết bị
        } catch (IOException e) {
            LOGGER.error("Hệ thống gặp lỗi nghiêm trọng trong tiến trình nạp tài nguyên tệp tin giao diện /Main.fxml", e);
        }
    }
}