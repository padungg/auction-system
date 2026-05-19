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

    private void handlePushNotification(com.google.gson.JsonObject push) {
        if (notifList == null) return;

        String eventType = push.has("event") ? push.get("event").getAsString() : "";

        if ("BID_UPDATE".equals(eventType)) {
            String bidder = push.has("bidder") ? push.get("bidder").getAsString() : "Khách";
            double price = push.has("price") ? push.get("price").getAsDouble() : 0.0;
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

            String text = "🔥 " + bidder + " vừa đặt giá mới " + String.format("%,.0f VNĐ", price) + " cho một sản phẩm!";

            VBox newNotif = createMockNotif(text, time, true);
            notifList.getChildren().add(0, newNotif); // Add to top

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

    private VBox createMockNotif(String text, String time, boolean unread) {
        VBox box = new VBox(5);
        box.getStyleClass().add("notif-item");
        if (unread) {
            box.getStyleClass().add("notif-unread");
        }

        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;"); // Will be overridden by dark-mode CSS

        Label timeLabel = new Label("⏰ " + time);
        timeLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        box.getChildren().addAll(msgLabel, timeLabel);
        return box;
    }

    public void updateUserInfo() {
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            headerUsername.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
            headerGreeting.setText("Xin chào, " + user.getUsername() + "!");
            headerBalance.setText("💰 " + String.format("%,.0f", user.getBalance()) + " VNĐ");
            headerAvatar.setText(user.getUsername().substring(0, 1).toUpperCase());

            // Show/Hide admin section
            boolean isAdmin = user.getRole() == UserRole.ADMIN;
            adminNav.setVisible(isAdmin);
            adminNav.setManaged(isAdmin);
        }
    }

    public void updateHeaderBalance(double balance) {
        headerBalance.setText("💰 " + String.format("%,.0f", balance) + " VNĐ");
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            user.setBalance(balance);
        }
    }

    /**
     * Gọi Server lấy thông tin mới nhất từ DB, cập nhật SessionManager và header.
     * Gọi sau mọi giao dịch tài chính (thanh toán, nạp, rút).
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

    @FXML
    public void showPageList(ActionEvent event) {
        setActiveNav(navList, "Danh sách đấu giá");
        loadPage("auction_list.fxml");
    }

    @FXML
    public void showPageDetail(ActionEvent event) {
        setActiveNav(navDetail, "Chi tiết sản phẩm");
        // Usually called from AuctionListController with a specific ID
    }

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

    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().clear();
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) contentStack.getScene().getWindow();

            // Fix: Tắt maximize trước khi setResizable(false) để tránh lỗi vỡ UI hoặc mất thanh tiêu đề
            stage.setMaximized(false);

            stage.setScene(new Scene(loginRoot));

            // Trả lại kích thước chuẩn của màn hình đăng nhập
            stage.setWidth(1000);
            stage.setHeight(650);

            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình Đăng xuất hệ thống", e);
        }
    }

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

    @FXML
    public void toggleNotifications(ActionEvent event) {
        boolean isVisible = notifDropdown.isVisible();
        notifDropdown.setVisible(!isVisible);
        notifDropdown.setManaged(!isVisible);
    }

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