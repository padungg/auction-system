package com.auction.client.controller;

import com.auction.client.util.SessionManager;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.UserRole;
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
 * Controller trung tâm điều phối cấu trúc giao diện nền tảng.
 */
public class MainController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    // THÔNG TIN NGƯỜI DÙNG TRÊN THANH ĐẦU TRANG FXML
    @FXML
    private Label headerTitle;
    @FXML
    private Label headerGreeting;
    @FXML
    private Label headerBalance;
    @FXML
    private Label headerAvatar;
    @FXML
    private Label headerUsername;
    @FXML
    private Label lblClock;
    @FXML
    private StackPane contentStack;

    // GIAO DIỆN CHỦ ĐỀ & THÔNG BÁO ĐẨY FXML
    @FXML
    private StackPane rootPane;
    @FXML
    private Button btnThemeToggle;
    @FXML
    private VBox notifDropdown;
    @FXML
    private Label notifBadge;
    @FXML
    private VBox notifList;

    private boolean isDarkMode = false;
    private int unreadCount = 0;

    // THANH CHUYỂN TRANG ĐIỀU HƯỚNG FXML
    @FXML
    private Button navList;
    @FXML
    private Button navDetail;
    @FXML
    private Button navManage;
    @FXML
    private Button navAccount;
    @FXML
    private Button navPayment;
    @FXML
    private Button navAdminUsers;
    @FXML
    private Button navAdminAuctions;
    @FXML
    private VBox adminNav;
    @FXML
    private Button btnBack;

    private AuctionDetailController currentDetailController;
    private static MainController instance;

    public static MainController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;
        updateUserInfo();
        setupRealtimeNotifications();
        startClock();
        showPageList();
    }

    /**
     * Vận hành đồng hồ thời gian thực.
     */
    private void startClock() {
        if (lblClock == null)
            return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, _ -> lblClock.setText("🕒 " + LocalDateTime.now().format(formatter))),
                new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    /**
     * Cấu hình ban đầu cho hệ thống thông báo đẩy.
     */
    private void setupRealtimeNotifications() {
        if (notifList != null) {
            notifList.getChildren().clear();
            notifBadge.setVisible(false);
            notifBadge.setText("0");
        }

        com.auction.client.network.ClientSocketManager.getInstance().addObserver(
                (_, _, payload) -> handlePushNotification(payload));
    }

    /**
     * Phân tích và hiển thị thông báo đẩy từ server.
     */
    private void handlePushNotification(com.google.gson.JsonObject push) {
        if (notifList == null)
            return;

        String eventType = push.has("event") ? push.get("event").getAsString() : "";

        if ("BID_UPDATE".equals(eventType)) {
            String bidder = push.has("bidderName") ? push.get("bidderName").getAsString()
                    : push.has("bidder") ? push.get("bidder").getAsString() : "Khách";
            double price = push.has("newPrice") ? push.get("newPrice").getAsDouble()
                    : push.has("price") ? push.get("price").getAsDouble() : 0.0;
            String product = push.has("itemName") ? push.get("itemName").getAsString() : "một sản phẩm";
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

            String text = "🔥 " + bidder + " vừa đặt giá "
                    + String.format("%,.0f VNĐ", price)
                    + " cho sản phẩm " + product;

            VBox newNotif = createMockNotif(text, time);
            notifList.getChildren().addFirst(newNotif);

            unreadCount++;
            notifBadge.setText(String.valueOf(unreadCount));
            notifBadge.setVisible(true);

        } else if ("AUCTION_CLOSED".equals(eventType)) {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String text = "⏹ Một phiên đấu giá vừa kết thúc!";

            VBox newNotif = createMockNotif(text, time);
            notifList.getChildren().addFirst(newNotif);

            unreadCount++;
            notifBadge.setText(String.valueOf(unreadCount));
            notifBadge.setVisible(true);
        }
    }

    /**
     * Khởi tạo card thông báo mới.
     */
    private VBox createMockNotif(String text, String time) {
        VBox box = new VBox(5);
        box.getStyleClass().addAll("notif-item", "notif-unread");

        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.getStyleClass().add("notif-msg");

        Label timeLabel = new Label("⏰ " + time);
        timeLabel.getStyleClass().add("notif-time");

        box.getChildren().addAll(msgLabel, timeLabel);
        return box;
    }

    /**
     * Đồng bộ hồ sơ tài khoản người dùng từ Session.
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
     * Cập nhật số dư tài khoản trên header.
     */
    public void updateHeaderBalance(double balance) {
        headerBalance.setText("💰 " + String.format("%,.0f", balance) + " VNĐ");
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            user.setBalance(balance);
        }
    }

    /**
     * Tải mới số dư tài khoản từ Database server.
     */
    public void refreshBalanceFromServer() {
        com.auction.client.network.ClientSocketManager.getInstance().execute(() -> {
            try {
                com.auction.model.protocol.Request req = new com.auction.model.protocol.Request(
                        com.auction.model.protocol.RequestType.GET_MY_PROFILE, null);
                com.auction.model.protocol.Response res = com.auction.client.network.ClientSocketManager.getInstance()
                        .sendRequest(req);

                if (res != null && res.getStatus() == com.auction.model.protocol.ResponseStatus.SUCCESS) {
                    UserResponseDTO fresh = res.getPayloadAs(UserResponseDTO.class);
                    if (fresh != null) {
                        SessionManager.getInstance().setCurrentUser(fresh);
                        javafx.application.Platform.runLater(() -> {
                            headerBalance.setText("💰 " + String.format("%,.0f", fresh.getBalance()) + " VNĐ");
                            LOGGER.info("[MainController] REFRESH: balance={} VNĐ",
                                    String.format("%,.0f", fresh.getBalance()));
                        });
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[MainController] Lỗi refresh balance", e);
            }
        });
    }

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

    private void setActiveNav(Button btn, String title) {
        resetNavButtons();
        btn.getStyleClass().removeAll("nav-button");
        btn.getStyleClass().add("nav-button-active");
        headerTitle.setText(title);
        if (btnBack != null) {
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        }
    }

    public void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlPath));
            Parent page = loader.load();
            contentStack.getChildren().setAll(page);
        } catch (Exception e) {
            LOGGER.error("[MainController] Lỗi load trang: {}", fxmlPath, e);
        }
    }

    /**
     * Hiển thị phân hệ Danh sách phiên đấu giá.
     */
    @FXML
    public void showPageList() {
        setActiveNav(navList, "Danh sách đấu giá");
        navDetail.setVisible(false);
        navDetail.setManaged(false);
        btnBack.setVisible(false);
        btnBack.setManaged(false);
        loadPage("AuctionList.fxml");
    }

    /**
     * Xử lý nút quay lại.
     */
    @FXML
    public void handleBack() {
        if (currentDetailController != null) {
            currentDetailController.goBack();
            currentDetailController = null;
        } else {
            showPageList();
        }
    }

    @FXML
    public void showPageDetail() {
        setActiveNav(navDetail, "Chi tiết sản phẩm");
    }

    /**
     * Định tuyến trực tiếp vào phòng chi tiết phiên đấu giá.
     */
    public void openAuctionDetail(String auctionId) {
        setActiveNav(navDetail, "Chi tiết sản phẩm");
        navDetail.setVisible(true);
        navDetail.setManaged(true);
        btnBack.setVisible(true);
        btnBack.setManaged(true);
        btnBack.setDisable(false);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AuctionDetail.fxml"));
            Parent page = loader.load();
            AuctionDetailController controller = loader.getController();
            currentDetailController = controller;
            controller.initData(auctionId);
            contentStack.getChildren().setAll(page);
        } catch (IOException e) {
            LOGGER.error("Không thể mở chi tiết sản phẩm cho phiên đấu giá: {}", auctionId, e);
        }
    }

    @FXML
    public void showPageManage() {
        setActiveNav(navManage, "Quản lý sản phẩm");
        loadPage("ManageSeller.fxml");
    }

    @FXML
    public void showPageAccount() {
        setActiveNav(navAccount, "Tài khoản của tôi");
        loadPage("Account.fxml");
    }

    @FXML
    public void showPagePayment() {
        setActiveNav(navPayment, "Thanh toán");
        loadPage("Payment.fxml");
    }

    @FXML
    public void showPageAdminUsers() {
        setActiveNav(navAdminUsers, "Quản lý người dùng");
        loadPage("AdminUsers.fxml");
    }

    @FXML
    public void showPageAdminAuctions() {
        setActiveNav(navAdminAuctions, "Quản lý đấu giá");
        loadPage("AdminAuctions.fxml");
    }

    /**
     * Xử lý Đăng xuất tài khoản hệ thống.
     */
    @FXML
    public void handleLogout() {
        SessionManager.getInstance().clear();
        try {
            URL resource = getClass().getResource("/Login.fxml");
            if (resource == null) {
                throw new IOException("Không tìm thấy tệp tài nguyên thiết kế giao diện /Login.fxml");
            }
            Parent loginRoot = FXMLLoader.load(resource);
            configureLoginStage(loginRoot);
        } catch (IOException e) {
            LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình Đăng xuất hệ thống", e);
        }
    }

    /**
     * Thiết lập cấu hình hình học của Stage phục vụ Login.
     */
    private void configureLoginStage(Parent loginRoot) {
        Stage stage = (Stage) contentStack.getScene().getWindow();
        stage.setMaximized(false);
        stage.setScene(new Scene(loginRoot));
        stage.setWidth(1000);
        stage.setHeight(650);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    /**
     * Hoán đổi chủ đề sáng/tối.
     */
    @FXML
    public void toggleTheme() {
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
     * Ẩn/hiển thị thanh danh sách thông báo đẩy.
     */
    @FXML
    public void toggleNotifications() {
        boolean isVisible = notifDropdown.isVisible();
        notifDropdown.setVisible(!isVisible);
        notifDropdown.setManaged(!isVisible);
    }

    /**
     * Đánh dấu toàn bộ thông báo đẩy là đã đọc.
     */
    @FXML
    public void markAllNotifsRead() {
        unreadCount = 0;
        notifBadge.setVisible(false);
        notifBadge.setText("0");

        for (Node node : notifList.getChildren()) {
            node.getStyleClass().remove("notif-unread");
        }
    }
}