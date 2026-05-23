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
 * <h2>MainController</h2>
 * <p>
 * Controller trung tâm điều phối cấu trúc giao diện nền tảng (Main Shell Layout) của ứng dụng phía Client.
 * </p>
 *
 * <p><b>Các đặc quyền nghiệp vụ cốt lõi:</b></p>
 * <ul>
 *   <li><b>Kiểm soát kiến trúc View (Page Router Engine):</b> Quản lý nạp động (Dynamic FXML Loading) các phân hệ màn hình con vào vùng chứa trung tâm `contentStack`.</li>
 *   <li><b>Đồng bộ phiên và số dư (Session Dashboard Core):</b> Giám sát thông tin định danh cá nhân, phân quyền truy cập Sidebar, cập nhật và đồng bộ số dư tài khoản thời gian thực từ máy chủ.</li>
 *   <li><b>Hệ thống thông báo đẩy (Real-time Notification Engine):</b> Đăng ký lắng nghe kênh Socket, tự động phân tích gói tin Json sự kiện đấu giá và cập nhật thanh thả xuống (Dropdown List) kèm bộ đếm Badge chưa đọc.</li>
 *   <li><b>Tiện ích hệ thống:</b> Vận hành đồng hồ hệ thống chính xác theo giây qua luồng Timeline độc lập và điều khiển hoán đổi chủ đề giao diện (Theme Toggle Light/Dark Mode).</li>
 * </ul>
 *
 * @since 1.0
 * @see javafx.fxml.Initializable
 * @see com.auction.client.network.ClientSocketManager
 */
public class MainController implements Initializable {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Chịu trách nhiệm theo dõi lịch sử nạp trang, cô lập lỗi luồng thông báo đẩy và giám sát hoạt động hệ thống.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - KHỐI TIÊU ĐỀ & THÔNG TIN HEADER
    // =========================================================================
    @FXML private Label headerTitle;
    @FXML private Label headerGreeting;
    @FXML private Label headerBalance;
    @FXML private Label headerAvatar;
    @FXML private Label headerUsername;
    @FXML private Label lblClock;
    @FXML private StackPane contentStack;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - KHỐI TIỆN ÍCH CHỦ ĐỀ & THÔNG BÁO (DROP DOWN)
    // =========================================================================
    @FXML private StackPane rootPane;
    @FXML private Button btnThemeToggle;
    @FXML private VBox notifDropdown;
    @FXML private Label notifBadge;
    @FXML private VBox notifList;

    /** Cờ giám sát trạng thái chuyển đổi chủ đề hiển thị (True: Dark Mode, False: Light Mode). */
    private boolean isDarkMode = false;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - NHÓM NÚT ĐIỀU HƯỚNG THANH SIDEBAR
    // =========================================================================
    @FXML private Button navList;
    @FXML private Button navDetail;
    @FXML private Button navManage;
    @FXML private Button navAccount;
    @FXML private Button navPayment;
    @FXML private Button navAdminUsers;
    @FXML private Button navAdminAuctions;
    @FXML private VBox adminNav;
    @FXML private VBox sellerNav;
    @FXML private Button btnBack;

    /** Tham chiếu đến Controller của trang chi tiết hiện tại, dùng để giải phóng Observer và Timeline đếm ngược. */
    private AuctionDetailController currentDetailController;

    /** Thực thể tĩnh duy nhất phục vụ mẫu thiết kế UI Singleton pattern. */
    private static MainController instance;

    /**
     * Phương thức truy xuất thực thể hiện hành (Singleton Instance Accessor).
     * Cho phép các Controller con hoặc các lớp tiện ích ngoại vi can thiệp điều phối chuyển trang từ xa.
     *
     * @return {@link MainController} Thực thể quản lý giao diện chính
     */
    public static MainController getInstance() {
        return instance;
    }

    /**
     * Phương thức khởi tạo vòng đời JavaFX View (Lifecycle Hook).
     * Được tự động kích hoạt ngay sau khi cây phân cấp đồ họa tệp FXML nạp thành công.
     * Thiết lập liên kết Instance, làm mới hồ sơ cá nhân, kích hoạt luồng sự kiện Socket và hiển thị trang mặc định.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;
        updateUserInfo();
        setupRealtimeNotifications();
        startClock();
        showPageList(null); // Tải mặc định màn hình Danh sách phiên đấu giá khi khởi động
    }

    /**
     * Kích hoạt và vận hành động cơ đồng hồ thời gian thực (Clock Engine).
     * Khởi tạo chu kỳ lặp vô hạn tần suất 1 giây dựa trên JavaFX Timeline nhằm cập nhật chuỗi text Giờ:Phút:Giây.
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

    /** Bộ đếm lưu giữ số lượng bản tin thông báo đẩy chưa xử lý đọc. */
    private int unreadCount = 0;

    /**
     * Thiết lập cấu hình ban đầu cho Container thông báo và đăng ký hàm phản hồi sự kiện
     * vào hệ thống phân phối tin mạng tập trung thông qua cơ chế Callback Observer.
     */
    private void setupRealtimeNotifications() {
        if (notifList != null) {
            notifList.getChildren().clear();
            notifBadge.setVisible(false);
            notifBadge.setText("0");
        }

        // Đăng ký bộ lắng nghe sự kiện luồng mạng real-time đẩy về từ Server
        com.auction.client.network.ClientSocketManager.getInstance().addObserver((event, auctionId, payload) -> {
            handlePushNotification(payload);
        });
    }

    /**
     * Động cơ phân tích bản tin thông báo đẩy (Push Notification Parsing Engine).
     * Trích xuất các trường dữ liệu Json từ Server, tạo chuỗi thông điệp tương ứng theo từng mã định danh loại sự kiện
     * (Đặt giá mới, Đóng phiên) và chuyển giao tiến trình dựng cấu trúc Node đồ họa lên giao diện.
     */
    private void handlePushNotification(com.google.gson.JsonObject push) {
        if (notifList == null) return;

        String eventType = push.has("event") ? push.get("event").getAsString() : "";

        // Thao tác phân tích loại sự kiện có người cập nhật lượt giá đặt mới
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

            VBox newNotif = createMockNotif(text, time, true);
            notifList.getChildren().add(0, newNotif); // Chèn phần tử mới lên vị trí đầu tiên của danh sách thả xuống

            unreadCount++;
            notifBadge.setText(String.valueOf(unreadCount));
            notifBadge.setVisible(true);

            // Thao tác phân tích loại sự kiện cưỡng chế đóng hoặc kết thúc thời gian phòng phiên
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
     * Nhà máy sản xuất Node thông báo đồ họa (Flyweight Notification Card Factory).
     * Khởi tạo cấu trúc Container lồng nhau (VBox) bọc các Label thông điệp, mốc thời gian
     * và đính kèm class phong cách CSS dựa vào trạng thái đã đọc hay chưa đọc.
     */
    private VBox createMockNotif(String text, String time, boolean unread) {
        VBox box = new VBox(5);
        box.getStyleClass().add("notif-item");
        if (unread) {
            box.getStyleClass().add("notif-unread"); // Đính kèm style CSS làm sáng nền bản tin chưa đọc
        }

        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true); // Kích hoạt cơ chế tự động xuống dòng khi chuỗi text vượt biên hiển thị
        msgLabel.getStyleClass().add("notif-msg");

        Label timeLabel = new Label("⏰ " + time);
        timeLabel.getStyleClass().add("notif-time");

        box.getChildren().addAll(msgLabel, timeLabel);
        return box;
    }

    /**
     * Đồng bộ hóa và cập nhật dữ liệu tài khoản người dùng từ Session Manager lên vùng Header.
     * Quản lý phân quyền hiển thị, tự động đóng/mở khối Container điều hướng dành riêng cho Quản trị viên (`adminNav`).
     */
    public void updateUserInfo() {
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            headerUsername.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
            headerGreeting.setText("Xin chào, " + user.getUsername() + "!");
            headerBalance.setText("💰 " + String.format("%,.0f", user.getBalance()) + " VNĐ");
            headerAvatar.setText(user.getUsername().substring(0, 1).toUpperCase());

            // Thực hiện kiểm tra quyền hạn tài khoản để cấu hình không gian hiển thị thanh Sidebar Admin
            boolean isAdmin = user.getRole() == UserRole.ADMIN;
            adminNav.setVisible(isAdmin);
            adminNav.setManaged(isAdmin);
        }
    }

    /**
     * Cập nhật văn bản hiển thị số dư tài khoản trên thanh tiêu đề, đồng thời đồng bộ giá trị
     * vào Session Cache bộ nhớ tạm Client nhằm duy trì tính nhất quán dữ liệu giữa các View.
     *
     * @param balance Chỉ số số dư tài khoản mới cần cập nhật hiển thị
     */
    public void updateHeaderBalance(double balance) {
        headerBalance.setText("💰 " + String.format("%,.0f", balance) + " VNĐ");
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            user.setBalance(balance);
        }
    }

    /**
     * Khởi chạy Worker Thread nền kết nối mạng gửi yêu cầu cập nhật hồ sơ cá nhân thời gian thực từ Database máy chủ.
     * Làm mới Session và ép tiến trình cập nhật text số dư trên Header về luồng giao diện an toàn an tâm tránh sai số dòng tiền.
     */
    public void refreshBalanceFromServer() {
        com.auction.client.network.ClientSocketManager.getInstance().execute(() -> {
            try {
                com.auction.model.protocol.Request req = new com.auction.model.protocol.Request(
                        com.auction.model.protocol.RequestType.GET_MY_PROFILE, null);
                com.auction.model.protocol.Response res =
                        com.auction.client.network.ClientSocketManager.getInstance().sendRequest(req);

                if (res != null && res.getStatus() == com.auction.model.protocol.ResponseStatus.SUCCESS) {
                    UserResponseDTO fresh = res.getPayloadAs(UserResponseDTO.class);
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
        });
    }

    /**
     * Dọn dẹp danh mục các lớp định dạng phong cách hoạt động (Active Style Classes)
     * của toàn bộ hệ thống nhóm nút bấm điều hướng Sidebar về trạng thái cơ bản.
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
     * Đánh dấu phong cách lựa chọn (Active CSS Style) cho nút bấm Sidebar chỉ định,
     * thiết lập văn bản tiêu đề Header phân hệ và kiểm soát ẩn hiện nút Quay lại (Back Button).
     */
    private void setActiveNav(Button btn, String title) {
        resetNavButtons();
        btn.getStyleClass().removeAll("nav-button");
        btn.getStyleClass().add("nav-button-active"); // Gán lớp CSS sáng màu nhận diện tab hoạt động
        headerTitle.setText(title);
        if (btnBack != null) {
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        }
    }

    /**
     * Công cụ nạp động tài nguyên phân hệ (Dynamic Page Loader Core).
     * Sử dụng FXMLLoader giải mã tệp tin thiết kế UI đích cấu hình và chèn thay thế toàn bộ
     * cấu trúc cây Node đồ họa con bên trong vùng chứa trung tâm `contentStack`.
     *
     * @param fxmlPath Đường dẫn chuỗi văn bản trỏ tới tệp tin FXML đích nằm trong thư mục resources
     */
    public void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlPath));
            Parent page = loader.load();
            contentStack.getChildren().setAll(page); // Thay thế Node đồ họa cũ bằng trang mới nạp
        } catch (Exception e) {
            LOGGER.error("[MainController] Lỗi load trang: {}", fxmlPath, e);
        }
    }

    /**
     * Điều hướng hiển thị màn hình phân hệ Danh sách các phiên đấu giá công khai.
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void showPageList(ActionEvent event) {
        setActiveNav(navList, "Danh sách đấu giá");
        navDetail.setVisible(false);
        navDetail.setManaged(false);
        btnBack.setVisible(false);
        btnBack.setManaged(false);
        loadPage("AuctionList.fxml");
    }

    /**
     * Đón nhận và điều phối hành động nhấp nút Quay lại (Back Button Click Action).
     * Chặn kiểm tra nếu đang rút lui khỏi trang Chi tiết phiên đấu giá, thực hiện kích hoạt hàm gỡ bỏ
     * vòng đời ngầm của trang đích (Dừng bộ đếm ngược Timeline, Hủy gán Observer) để tránh rò rỉ bộ nhớ RAM.
     *
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void handleBack(ActionEvent event) {
        if (currentDetailController != null) {
            currentDetailController.goBack(); // Kích hoạt chuỗi giải phóng tài nguyên của trang chi tiết sản phẩm
            currentDetailController = null;
        } else {
            showPageList(null);
        }
    }

    /**
     * Đón nhận hành động nhấn nút di chuyển hiển thị danh mục Chi tiết sản phẩm từ Sidebar.
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void showPageDetail(ActionEvent event) {
        setActiveNav(navDetail, "Chi tiết sản phẩm");
    }

    /**
     * Định tuyến và khởi tạo phân hệ phòng phiên đấu giá chi tiết cụ thể (Deep Link Page Routing).
     * Bật nhãn định hướng thanh Sidebar, cấu hình kích hoạt hiển thị nút Back, tải tài nguyên biểu mẫu,
     * lưu vết tham chiếu Controller con và truyền mã định danh khởi chạy cấu hình nạp mạng dữ liệu thời gian thực.
     *
     * @param auctionId Mã định danh duy nhất của phiên đấu giá mục tiêu cần truy cập phòng phiên
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
            currentDetailController = controller; // Đăng ký lưu vết tham chiếu phục vụ gỡ bỏ vòng đời khi Back
            controller.initData(auctionId); // Kích hoạt truyền dữ liệu kết nối mạng bất đồng bộ
            contentStack.getChildren().setAll(page);
        } catch (IOException e) {
            LOGGER.error("Không thể mở chi tiết sản phẩm cho phiên đấu giá: {}", auctionId, e);
        }
    }

    /**
     * Điều hướng hiển thị màn hình phân hệ Quản lý sản phẩm dành cho Người đăng bán (Seller Panel).
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void showPageManage(ActionEvent event) {
        setActiveNav(navManage, "Quản lý sản phẩm");
        loadPage("ManageSeller.fxml");
    }

    /**
     * Điều hướng hiển thị màn hình phân hệ Hồ sơ cá nhân và lịch sử đấu giá thành viên (Account Profile).
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void showPageAccount(ActionEvent event) {
        setActiveNav(navAccount, "Tài khoản của tôi");
        loadPage("Account.fxml");
    }

    /**
     * Điều hướng hiển thị màn hình phân hệ Giao dịch tài chính (Payment Dashboard).
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void showPagePayment(ActionEvent event) {
        setActiveNav(navPayment, "Thanh toán");
        loadPage("Payment.fxml");
    }

    /**
     * Điều hướng hiển thị màn hình phân hệ Quản trị danh sách người dùng dành cho Admin (Admin Users).
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void showPageAdminUsers(ActionEvent event) {
        setActiveNav(navAdminUsers, "Quản lý người dùng");
        loadPage("AdminUsers.fxml");
    }

    /**
     * Điều hướng hiển thị màn hình phân hệ Quản lý điều khiển danh sách phiên đấu giá toàn cục dành cho Admin (Admin Auctions).
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void showPageAdminAuctions(ActionEvent event) {
        setActiveNav(navAdminAuctions, "Quản lý đấu giá");
        loadPage("AdminAuctions.fxml");
    }

    /**
     * Đón nhận hành động xử lý Đăng xuất tài khoản hệ thống (Logout Action Handler).
     * Tiến hành dọn dẹp bộ nhớ cache Session, trích xuất cấu khống chế Stage cửa sổ, hoàn tác thuộc tính co giãn,
     * đặt lại kích cỡ khung hình cố định (1000x650) tránh vỡ giao diện đăng nhập và tải lại `/Login.fxml`.
     *
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().clear(); // Giải phóng hoàn toàn thông tin tài khoản phiên hiện hành khỏi cache RAM
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) contentStack.getScene().getWindow();

            stage.setMaximized(false); // Hủy bỏ chế độ phóng to toàn màn hình tránh tràn vỡ khung Form đăng nhập cố định
            stage.setScene(new Scene(loginRoot));

            // Thiết lập khống chế kích thước hình học tiêu chuẩn dành cho biểu mẫu Đăng nhập/Đăng ký
            stage.setWidth(1000);
            stage.setHeight(650);

            stage.setResizable(false); // Khóa quyền co giãn kích thước cửa sổ ở màn hình xác thực
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình Đăng xuất hệ thống", e);
        }
    }

    /**
     * Điều khiển hoán đổi chủ đề đồ họa hiển thị giao diện (Theme Toggle Switcher).
     * Thực hiện đính kèm hoặc bóc tách lớp định danh CSS Style tổng `.dark-mode` trên nút thắt gốc Root Pane.
     *
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
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
     * Điều khiển ẩn hoặc hiển thị hộp thoại khung thả xuống danh sách thông báo đẩy (Toggle Dropdown Pane).
     *
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void toggleNotifications(ActionEvent event) {
        boolean isVisible = notifDropdown.isVisible();
        notifDropdown.setVisible(!isVisible);
        notifDropdown.setManaged(!isVisible);
    }

    /**
     * Đón nhận hành động Đánh dấu tất cả thông báo là đã đọc (Mark All Notifications As Read).
     * Thực hiện đặt lại bộ đếm chỉ số về 0, xóa bỏ cờ phong cách CSS chưa đọc (`notif-unread`)
     * trên toàn bộ hệ thống cây Node con lưu trữ trong Container bản tin thả xuống.
     *
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void markAllNotifsRead(ActionEvent event) {
        unreadCount = 0;
        notifBadge.setVisible(false);
        notifBadge.setText("0");

        // Vòng lặp duyệt qua toàn bộ tập hợp Node đồ họa con để loại bỏ trạng thái class CSS highlight unread
        for (Node node : notifList.getChildren()) {
            node.getStyleClass().remove("notif-unread");
        }
    }
}