package com.auction.client.controller;

import com.auction.client.util.SessionManager;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.UserRole;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bộ điều khiển chính (Main Controller) của giao diện ứng dụng.
 * Chịu trách nhiệm quản lý layout tổng thể, thanh điều hướng (Sidebar),
 * đồng bộ thông tin phiên làm việc, đồng hồ hệ thống và trung tâm thông báo thời gian thực.
 */
public class MainController implements Initializable {

    /**
     * Khởi tạo hệ thống ghi nhật ký log theo tiêu chuẩn SLF4J nhằm phục vụ công tác giám sát luồng vận hành,
     * theo dõi các sự kiện đẩy thông báo thời gian thực và quản lý các tác vụ ngắt kết nối hệ thống.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @FXML private Label headerTitle;
    @FXML private Label headerGreeting;
    @FXML private Label headerBalance;
    @FXML private Label headerAvatar;
    @FXML private Label headerUsername;
    @FXML private Label lblClock;
    @FXML private StackPane contentStack;

    @FXML private StackPane rootPane;
    @FXML private Button btnThemeToggle;
    @FXML private VBox notifDropdown;
    @FXML private Label notifBadge;
    @FXML private VBox notifList;

    private boolean isDarkMode = false;

    @FXML private Button navList;
    @FXML private Button navDetail;
    @FXML private Button navManage;
    @FXML private Button navAccount;
    @FXML private Button navPayment;
    @FXML private Button navAdminUsers;
    @FXML private Button navAdminAuctions;
    @FXML private VBox adminNav;
    @FXML private VBox sellerNav;

    private static MainController instance;

    /**
     * Lấy thực thể (instance) duy nhất của MainController để các bộ điều khiển con có thể gọi và chuyển trang.
     * Áp dụng theo dạng cấu trúc Singleton tạm thời cho tầng UI.
     */
    public static MainController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;
        updateUserInfo();
        setupRealtimeNotifications();
        startClock();
        showPageList(null);
    }

    /**
     * Kích hoạt luồng chạy tuyến tính (Timeline) để cập nhật hiển thị thời gian thực cho đồng hồ hệ thống.
     */
    private void startClock() {
        if (lblClock == null) return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblClock.setText("🕒 " + LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private int unreadCount = 0;

    /**
     * Thiết lập cấu hình ban đầu cho danh sách và đăng ký bộ lắng nghe sự kiện (Listener)
     * để đón nhận các gói tin thông báo thời gian thực đẩy về từ Socket Server.
     */
    private void setupRealtimeNotifications() {
        if (notifList != null) {
            notifList.getChildren().clear();
            notifBadge.setVisible(false);
            notifBadge.setText("0");
        }

        com.auction.client.network.ClientSocketManager.getInstance().setNotificationListener(push -> {
            javafx.application.Platform.runLater(() -> {
                handlePushNotification(push);
            });
        });
    }

    /**
     * Phân tích gói tin Json thông báo nhận được từ Server và dựng các thẻ thông báo đồ họa lồng nhau lên giao diện Client.
     */
    private void handlePushNotification(com.google.gson.JsonObject push) {
        if (notifList == null) return;

        String eventType = push.has("event") ? push.get("event").getAsString() : "";

        if ("BID_UPDATE".equals(eventType)) {
            String bidder = push.has("bidder") ? push.get("bidder").getAsString() : "Khách";
            double price = push.has("price") ? push.get("price").getAsDouble() : 0.0;
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

            String text = "🔥 " + bidder + " vừa đặt giá mới " + String.format("%,.0f VNĐ", price) + " cho một sản phẩm!";

            VBox newNotif = createMockNotif(text, time, true);
            notifList.getChildren().add(0, newNotif); // Thêm lên đầu danh sách

            unreadCount++;
            notifBadge.setText(String.valueOf(unreadCount));
            notifBadge.setVisible(true);
        } else if ("AUCTION_CLOSED".equals(eventType)) {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String text = "⏹ Một phiên đấu giá vừa kết thúc!";

            VBox newNotif = createMockNotif(text, time, true);
            notifList.getChildren().add(0, newNotif);

            unreadCount++;
            notifBadge.setText(String.valueOf(unreadCount));
            notifBadge.setVisible(true);
        }
    }

    /**
     * Tạo một container đồ họa VBox cho từng dòng thông báo, hỗ trợ gán style CSS dạng chưa đọc (unread).
     */
    private VBox createMockNotif(String text, String time, boolean unread) {
        VBox box = new VBox(5);
        box.getStyleClass().add("notif-item");
        if (unread) {
            box.getStyleClass().add("notif-unread");
        }

        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");

        Label timeLabel = new Label("⏰ " + time);
        timeLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        box.getChildren().addAll(msgLabel, timeLabel);
        return box;
    }

    /**
     * Cập nhật thông tin người dùng từ Session Manager lên vùng Header (Tên hiển thị, Lời chào, Số dư, Avatar ký tự).
     * Tự động ẩn/hiện thanh điều hướng của Quản trị viên (Admin Nav) dựa trên phân quyền tài khoản.
     */
    public void updateUserInfo() {
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            headerUsername.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
            headerGreeting.setText("Xin chào, " + user.getUsername() + "!");
            headerBalance.setText("💰 " + String.format("%,.0f", user.getBalance()) + " VNĐ");
            headerAvatar.setText(user.getUsername().substring(0, 1).toUpperCase());

            boolean isAdmin = user.getRole() == UserRole.ADMIN;
            adminNav.setVisible(isAdmin);
            adminNav.setManaged(isAdmin);
        }
    }

    /**
     * Làm mới số dư hiển thị trên thanh tiêu đề và đồng bộ ngược lại vào Session Cache bộ nhớ tạm Client.
     */
    public void updateHeaderBalance(double balance) {
        headerBalance.setText("💰 " + String.format("%,.0f", balance) + " VNĐ");
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            user.setBalance(balance);
        }
    }

    /**
     * Khởi chạy Worker Thread nền để gửi yêu cầu lấy thông tin hồ sơ mới nhất từ Database của máy chủ.
     * Cập nhật trực tiếp vào SessionManager và render lại số dư thực tế lên Header nhằm tránh sai lệch dòng tiền.
     */
    public void refreshBalanceFromServer() {
        new Thread(() -> {
            try {
                com.auction.model.protocol.Request req = new com.auction.model.protocol.Request(
                        com.auction.model.protocol.RequestType.GET_MY_PROFILE, null);
                com.auction.model.protocol.Response res =
                        com.auction.client.network.ClientSocketManager.getInstance().sendRequest(req);

                if (res.getStatus() == com.auction.model.protocol.ResponseStatus.SUCCESS) {
                    com.google.gson.Gson gson =
                            com.auction.client.network.ClientSocketManager.getInstance().getGson();
                    UserResponseDTO fresh = gson.fromJson(
                            gson.toJson(res.getPayload()), UserResponseDTO.class);
                    if (fresh != null) {
                        SessionManager.getInstance().setCurrentUser(fresh);
                        javafx.application.Platform.runLater(() -> {
                            headerBalance.setText("💰 " + String.format("%,.0f", fresh.getBalance()) + " VNĐ");
                            LOGGER.info("[MainController] REFRESH: balance={} VNĐ", String.format("%,.0f", fresh.getBalance()));
                        });
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[MainController] Lỗi refresh balance", e);
            }
        }).start();
    }

    /**
     * Hoàn tác trạng thái hoạt động (Active CSS Style) của tất cả các nút bấm điều hướng trên Sidebar về mặc định.
     */
    private void resetNavButtons() {
        navList.getStyleClass().removeAll("nav-button-active");
        navList.getStyleClass().add("nav-button");

        navDetail.getStyleClass().removeAll("nav-button-active");
        navDetail.getStyleClass().add("nav-button");

        navManage.getStyleClass().removeAll("nav-button-active");
        navManage.getStyleClass().add("nav-button");

        navAccount.getStyleClass().removeAll("nav-button-active");
        navAccount.getStyleClass().add("nav-button");

        navPayment.getStyleClass().removeAll("nav-button-active");
        navPayment.getStyleClass().add("nav-button");

        navAdminUsers.getStyleClass().removeAll("nav-button-active");
        navAdminUsers.getStyleClass().add("nav-button");

        navAdminAuctions.getStyleClass().removeAll("nav-button-active");
        navAdminAuctions.getStyleClass().add("nav-button");
    }

    /**
     * Đánh dấu nút bấm đang được chọn trên Sidebar và cập nhật tiêu đề phân hệ tương ứng lên thanh điều phối chính.
     */
    private void setActiveNav(Button btn, String title) {
        resetNavButtons();
        btn.getStyleClass().removeAll("nav-button");
        btn.getStyleClass().add("nav-button-active");
        headerTitle.setText(title);
    }

    /**
     * Nạp động nội dung tệp tin cấu hình giao diện FXML đích vào trong phân vùng hiển thị chính (contentStack).
     */
    public void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlPath));
            Parent page = loader.load();
            contentStack.getChildren().setAll(page);
        } catch (Exception e) {
            LOGGER.error("[MainController] Lỗi load trang: {}", fxmlPath, e);
        }
    }

    @FXML
    public void showPageList(ActionEvent event) {
        setActiveNav(navList, "Danh sách đấu giá");
        loadPage("auction_list.fxml");
    }

    @FXML
    public void showPageDetail(ActionEvent event) {
        setActiveNav(navDetail, "Chi tiết sản phẩm");
    }

    /**
     * Chuyển hướng màn hình chính sang trang chi tiết của một phiên đấu giá cụ thể dựa trên ID sản phẩm,
     * đồng thời khởi tạo dữ liệu cho trang đích.
     */
    public void openAuctionDetail(String auctionId) {
        setActiveNav(navDetail, "Chi tiết sản phẩm");
        navDetail.setVisible(true);
        navDetail.setManaged(true);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_detail.fxml"));
            Parent page = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.initData(auctionId);
            contentStack.getChildren().setAll(page);
        } catch (IOException e) {
            LOGGER.error("Không thể mở chi tiết sản phẩm cho phiên đấu giá: {}", auctionId, e);
        }
    }

    @FXML
    public void showPageManage(ActionEvent event) {
        setActiveNav(navManage, "Quản lý sản phẩm");
        loadPage("manage_seller.fxml");
    }

    @FXML
    public void showPageAccount(ActionEvent event) {
        setActiveNav(navAccount, "Tài khoản của tôi");
        loadPage("account.fxml");
    }

    @FXML
    public void showPagePayment(ActionEvent event) {
        setActiveNav(navPayment, "Thanh toán");
        loadPage("payment.fxml");
    }

    @FXML
    public void showPageAdminUsers(ActionEvent event) {
        setActiveNav(navAdminUsers, "Quản lý người dùng");
        loadPage("admin_users.fxml");
    }

    @FXML
    public void showPageAdminAuctions(ActionEvent event) {
        setActiveNav(navAdminAuctions, "Quản lý đấu giá");
        loadPage("admin_auctions.fxml");
    }

    /**
     * Xử lý xóa bỏ phiên làm việc hiện tại, thu nhỏ cửa sổ tối đa (Maximize) để ngăn lỗi tràn khung
     * và trả người dùng về màn hình Đăng nhập (login.fxml) với kích thước chuẩn cố định.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().clear();
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) contentStack.getScene().getWindow();

            stage.setMaximized(false);
            stage.setScene(new Scene(loginRoot));

            stage.setWidth(1000);
            stage.setHeight(650);

            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình Đăng xuất hệ thống", e);
        }
    }

    /**
     * Chuyển đổi qua lại giữa Chế độ sáng (Light Mode) và Chế độ tối (Dark Mode) bằng việc cập nhật lớp CSS Style tổng.
     */
    @FXML
    public void toggleTheme(ActionEvent event) {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            rootPane.getStyleClass().add("dark-mode");
            btnThemeToggle.setText("☀️");
        } else {
            rootPane.getStyleClass().remove("dark-mode");
            btnThemeToggle.setText("🌙");
        }
    }

    /**
     * Ẩn hoặc hiện khung thả xuống (Dropdown) chứa danh sách các thông báo đẩy.
     */
    @FXML
    public void toggleNotifications(ActionEvent event) {
        boolean isVisible = notifDropdown.isVisible();
        notifDropdown.setVisible(!isVisible);
        notifDropdown.setManaged(!isVisible);
    }

    /**
     * Đánh dấu toàn bộ các thông báo hiện có trong danh sách thành trạng thái đã đọc,
     * xóa bỏ cờ đánh dấu chưa đọc (unread CSS class) và reset bộ đếm Badge về 0.
     */
    @FXML
    public void markAllNotifsRead(ActionEvent event) {
        unreadCount = 0;
        notifBadge.setVisible(false);
        notifBadge.setText("0");

        for (Node node : notifList.getChildren()) {
            node.getStyleClass().remove("notif-unread");
        }
    }
}