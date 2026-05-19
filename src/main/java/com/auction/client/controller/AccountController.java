package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.MyBidHistoryDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Controller chịu trách nhiệm quản lý phân hệ Hồ sơ cá nhân (Account Profile).
 * Đảm nhiệm hiển thị thông tin người dùng, lịch sử tham gia đặt giá, thực hiện các giao dịch tài chính
 * và đồng bộ hóa số dư tài khoản thời gian thực trên giao diện Client.
 */
public class AccountController {

    /**
     * Khởi tạo bộ ghi log tập trung phục vụ giám sát luồng thực thi ứng dụng.
     * Cung cấp khả năng phân tách mức độ nghiêm trọng của lỗi phần mềm (như lỗi kết nối Socket),
     * hỗ trợ kết xuất tệp tin nhật ký độc lập giúp tăng tốc độ tìm kiếm nguyên nhân sự cố trong môi trường Production.
     */
    private static final Logger logger = Logger.getLogger(AccountController.class.getName());

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - THÔNG TIN CÁ NHÂN (PROFILE LABELS)
    // =========================================================================
    @FXML private Label lblAvatar;
    @FXML private Label lblName;
    @FXML private Label lblUsername;
    @FXML private Label lblEmail;
    @FXML private Label lblRole;
    @FXML private Label lblPhone;
    @FXML private Label lblAddress;
    @FXML private Label lblStore;
    @FXML private Label lblRating;
    @FXML private Label lblBalance;
    @FXML private HBox boxStore;
    @FXML private TextField txtAmount;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - BẢNG LỊCH SỬ ĐẶT GIÁ (BID HISTORY TABLE)
    // =========================================================================
    @FXML private TableView<MyBidHistoryDTO> tableHistory;
    @FXML private TableColumn<MyBidHistoryDTO, String> colAuctionId;
    @FXML private TableColumn<MyBidHistoryDTO, String> colItemName;
    @FXML private TableColumn<MyBidHistoryDTO, Double> colPrice;
    @FXML private TableColumn<MyBidHistoryDTO, LocalDateTime> colTime;
    @FXML private TableColumn<MyBidHistoryDTO, String> colResult;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - ĐIỀU KHIỂN PHÂN TRANG (PAGINATION CONTROLS)
    // =========================================================================
    @FXML private HBox histPageBox;
    @FXML private Label histPageInfo;
    @FXML private Button histBtnFirst;
    @FXML private Button histBtnPrev;
    @FXML private Button histBtnNext;
    @FXML private Button histBtnLast;

    // Các tham số quản lý cấu trúc phân trang danh sách lịch sử
    private static final int HIST_PAGE_SIZE = 5;
    private int histCurrentPage = 0;
    private List<MyBidHistoryDTO> allHistory = new ArrayList<>();
    private ObservableList<MyBidHistoryDTO> bidHistory = FXCollections.observableArrayList();

    /**
     * Phương thức vòng đời JavaFX View (Lifecycle Hook).
     * Thiết lập cấu trúc hiển thị lưới dữ liệu, liên kết thông tin phiên người dùng và nạp dữ liệu mạng bất đồng bộ.
     */
    @FXML
    public void initialize() {
        setupTable();
        loadUserProfile();
        loadBidHistory();
    }

    // =========================================================================
    // KHỞI TẠO CẤU TRÚC HIỂN THỊ BẢNG (TABLEVIEW SETUP)
    // =========================================================================

    /**
     * Định hình quy tắc liên kết thuộc tính DTO và tùy biến giao diện CSS cho các Cell đặc thù trong bảng.
     */
    private void setupTable() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colItemName.setCellFactory(tc -> new TableCell<MyBidHistoryDTO, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(name);
                    setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                }
            }
        });

        colPrice.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colPrice.setCellFactory(tc -> new TableCell<MyBidHistoryDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colTime.setCellFactory(tc -> new TableCell<MyBidHistoryDTO, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    setText(time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                }
            }
        });

        // Thiết lập bộ phân cấp đồ họa Badge tương ứng với kết quả phiên đấu giá (Thắng, Thất bại, Đang đấu)
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colResult.setCellFactory(tc -> new TableCell<MyBidHistoryDTO, String>() {
            @Override
            protected void updateItem(String result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label();
                String base = "-fx-padding: 3 10; -fx-background-radius: 99; -fx-font-weight: bold; -fx-font-size: 11px; ";

                switch (result) {
                    case "Thắng":
                        badge.setText("🏆 Thắng");
                        badge.setStyle(base + "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
                        break;
                    case "Thất bại":
                        badge.setText("❌ Thất bại");
                        badge.setStyle(base + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
                        break;
                    case "Đang đấu":
                        badge.setText("⚡ Đang đấu");
                        badge.setStyle(base + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
                        break;
                    default:
                        badge.setText(result);
                        badge.setStyle(base + "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;");
                }

                setGraphic(badge);
                setAlignment(Pos.CENTER);
                setText(null);
            }
        });

        tableHistory.setItems(bidHistory);
        tableHistory.setFixedCellSize(48); // Khống chế kích thước dòng cố định để tối ưu hóa hiệu năng cuộn của lưới
    }

    // =========================================================================
    // ĐỒNG BỘ VÀ TẢI DỮ LIỆU TỪ HỆ THỐNG MẠNG (DATA LOADING)
    // =========================================================================

    /**
     * Giải nén thông tin người dùng hiện tại từ Memory Cache Session và ánh xạ trực tiếp lên cấu trúc Label UI.
     */
    private void loadUserProfile() {
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername();
            lblName.setText(name);
            lblAvatar.setText(name.substring(0, 1).toUpperCase());
            lblUsername.setText("@" + user.getUsername());
            lblEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa cập nhật");
            lblRole.setText(user.getRole() != null ? "Vai trò: " + user.getRole() : "MEMBER");
            lblPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Chưa cập nhật");
            lblAddress.setText(user.getAddress() != null && !user.getAddress().isEmpty() ? user.getAddress() : "Chưa cập nhật");

            if (user.getStoreName() != null && !user.getStoreName().isEmpty()) {
                boxStore.setVisible(true);
                boxStore.setManaged(true);
                lblStore.setText(user.getStoreName());
            }

            lblRating.setText(String.format("%.1f", user.getRating()));
            updateBalanceUI(user.getBalance());
        }
    }

    /**
     * Làm mới thông số hiển thị số dư tài khoản cá nhân, đồng thời phát tín hiệu đồng bộ lên Header chính.
     */
    private void updateBalanceUI(double balance) {
        lblBalance.setText(String.format("%,.0f VNĐ", balance));
        if (MainController.getInstance() != null) {
            MainController.getInstance().updateHeaderBalance(balance);
        }
    }

    /**
     * Kích hoạt một Worker Thread độc lập gửi chỉ thị mạng truy vấn danh sách lịch sử thao tác đặt giá của tài khoản hiện tại.
     */
    private void loadBidHistory() {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.GET_MY_BID_HISTORY, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res.getStatus() == ResponseStatus.SUCCESS) {
                    Gson gson = ClientSocketManager.getInstance().getGson();
                    List<MyBidHistoryDTO> history = gson.fromJson(
                            gson.toJson(res.getPayload()),
                            new com.google.gson.reflect.TypeToken<List<MyBidHistoryDTO>>(){}.getType()
                    );

                    Platform.runLater(() -> {
                        allHistory.clear();
                        if (history != null) allHistory.addAll(history);
                        histCurrentPage = 0;
                        renderHistPage();
                    });
                }
            } catch (Exception e) {
                // Thay thế câu lệnh in Console thô sơ bằng hệ thống ghi nhật ký cảnh báo SEVERE kèm cấu trúc vết ngoại lệ
                logger.log(Level.SEVERE, "Lỗi xảy ra trong tiến trình tải danh sách lịch sử đặt giá từ Server: ", e);
            }
        }).start();
    }

    // =========================================================================
    // THUẬT TOÁN VÀ LOGIC PHÂN TRANG UI (PAGINATION LOGIC)
    // =========================================================================

    /**
     * Tính toán lát cắt danh sách lịch sử dựa trên chỉ mục trang hiện hành và tiến hành render nhóm nút điều hướng động.
     */
    private void renderHistPage() {
        int total = allHistory.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / HIST_PAGE_SIZE));
        int from = histCurrentPage * HIST_PAGE_SIZE;
        int to   = Math.min(from + HIST_PAGE_SIZE, total);

        bidHistory.clear();
        bidHistory.addAll(allHistory.subList(from, to));

        if (histPageInfo != null) {
            histPageInfo.setText("Trang " + (histCurrentPage + 1) + " / " + totalPages + "  (" + total + " lượt)");
        }

        if (histBtnFirst != null) histBtnFirst.setDisable(histCurrentPage == 0);
        if (histBtnPrev  != null) histBtnPrev.setDisable(histCurrentPage == 0);
        if (histBtnNext  != null) histBtnNext.setDisable(histCurrentPage >= totalPages - 1);
        if (histBtnLast  != null) histBtnLast.setDisable(histCurrentPage >= totalPages - 1);

        // Khởi tạo và thiết lập định dạng CSS cho thanh nút bấm chỉ mục số trang động (Giới hạn tối đa 5 nút hiển thị)
        if (histPageBox != null) {
            histPageBox.getChildren().clear();
            int maxBtn = 5, startP = Math.max(0, histCurrentPage - 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn) startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                Button btn = new Button(String.valueOf(p + 1));
                btn.setStyle(p == histCurrentPage
                        ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 30; -fx-pref-width: 30;"
                        : "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 30; -fx-pref-width: 30;");
                btn.setOnAction(e -> { histCurrentPage = pg; renderHistPage(); });
                histPageBox.getChildren().add(btn);
            }
        }
    }

    // Nhóm các hàm định tuyến xử lý hành động kích hoạt nút điều chuyển phân trang nhanh
    @FXML public void histGoFirst(ActionEvent e) { histCurrentPage = 0; renderHistPage(); }
    @FXML public void histGoPrev(ActionEvent e)  { if (histCurrentPage > 0) { histCurrentPage--; renderHistPage(); } }
    @FXML public void histGoNext(ActionEvent e)  {
        int t = Math.max(1, (int) Math.ceil((double) allHistory.size() / HIST_PAGE_SIZE));
        if (histCurrentPage < t - 1) { histCurrentPage++; renderHistPage(); }
    }
    @FXML public void histGoLast(ActionEvent e)  {
        histCurrentPage = Math.max(0, (int) Math.ceil((double) allHistory.size() / HIST_PAGE_SIZE) - 1);
        renderHistPage();
    }

    // =========================================================================
    // XỬ LÝ BIỂU MẪU CẬP NHẬT HỒ SƠ (EDIT PROFILE DIALOG POPUP)
    // =========================================================================

    /**
     * Khởi tạo giao diện lưới (GridPane) tích hợp bên trong JavaFX Dialog để thu thập thông tin thay đổi hồ sơ từ người dùng.
     */
    @FXML
    void handleEditProfile(ActionEvent event) {
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa hồ sơ");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 30, 20, 30));
        grid.setPrefWidth(380);

        TextField tfName  = new TextField(user != null && user.getFullName() != null ? user.getFullName() : "");
        TextField tfPhone = new TextField(user != null && user.getPhone() != null ? user.getPhone() : "");
        TextField tfAddr  = new TextField(user != null && user.getAddress() != null ? user.getAddress() : "");
        TextField tfStore = new TextField(user != null && user.getStoreName() != null ? user.getStoreName() : "");
        PasswordField tfPass = new PasswordField();
        tfPass.setPromptText("Mật khẩu mới (bỏ trống nếu không đổi)");

        Label lblMsg = new Label();
        lblMsg.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        String inputStyle = "-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8; -fx-pref-width: 260;";
        tfName.setStyle(inputStyle);
        tfPhone.setStyle(inputStyle);
        tfAddr.setStyle(inputStyle);
        tfStore.setStyle(inputStyle);
        tfPass.setStyle(inputStyle);

        grid.add(new Label("Họ và tên *"),  0, 0); grid.add(tfName,  1, 0);
        grid.add(new Label("Số điện thoại"), 0, 1); grid.add(tfPhone, 1, 1);
        grid.add(new Label("Địa chỉ"),       0, 2); grid.add(tfAddr,  1, 2);
        grid.add(new Label("Tên cửa hàng"),  0, 3); grid.add(tfStore, 1, 3);
        grid.add(new Label("Đổi mật khẩu"),  0, 4); grid.add(tfPass,  1, 4);
        grid.add(lblMsg, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Thiết lập bộ quy tắc thẩm mỹ cho nút Lưu dữ liệu trên thanh hành động Dialog
        javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");

        dialog.showAndWait().ifPresent(result -> {
            if (result != saveBtn) return;

            String fullName  = tfName.getText().trim();
            String phone     = tfPhone.getText().trim();
            String address   = tfAddr.getText().trim();
            String storeName = tfStore.getText().trim();
            String password  = tfPass.getText().trim();

            if (fullName.isEmpty()) {
                lblMsg.setText("Họ và tên không được để trống!");
                return;
            }

            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("fullName", fullName);
            payload.put("phone", phone);
            payload.put("address", address);
            payload.put("storeName", storeName);
            if (!password.isEmpty()) payload.put("password", password);

            // Gửi cấu trúc Map cập nhật thuộc tính lên máy chủ thông qua kênh luồng mạng bất đồng bộ
            new Thread(() -> {
                try {
                    Request req = new Request(RequestType.UPDATE_PROFILE, payload);
                    Response res = ClientSocketManager.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        if (res.getStatus() == ResponseStatus.SUCCESS) {
                            try {
                                Gson gson = ClientSocketManager.getInstance().getGson();
                                UserResponseDTO updatedUser = gson.fromJson(gson.toJson(res.getPayload()), UserResponseDTO.class);
                                SessionManager.getInstance().setCurrentUser(updatedUser);
                            } catch (Exception ex) {
                                // Ghi nhận vết sự cố cục bộ khi cố gán cấu trúc thực thể người dùng vào Session bộ nhớ đệm
                                logger.log(Level.SEVERE, "Không thể cập nhật thông tin thực thể vào Session bộ nhớ đệm: ", ex);
                            }

                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật hồ sơ thành công!");
                            if (MainController.getInstance() != null) {
                                MainController.getInstance().loadPage("account.fxml");
                            }
                        } else {
                            showAlert(Alert.AlertType.WARNING, "Lỗi", res.getMessage() != null ? res.getMessage() : "Cập nhật thất bại!");
                        }
                    });
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Gặp sự cố ngắt kết nối mạng khi đang gửi yêu cầu UPDATE_PROFILE: ", ex);
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", ex.getMessage()));
                }
            }).start();
        });
    }

    // =========================================================================
    // NGHIỆP VỤ XỬ LÝ GIAO DỊCH TÀI CHÍNH (DEPOSIT / WITHDRAWAL)
    // =========================================================================

    @FXML void handleDeposit(ActionEvent event) { processTransaction(RequestType.DEPOSIT, "Nạp tiền"); }
    @FXML void handleWithdraw(ActionEvent event) { processTransaction(RequestType.WITHDRAW, "Rút tiền"); }

    /**
     * Quy trình xử lý đóng gói và thẩm định tính hợp lệ của số dư tiền tệ trước khi truyền phát chỉ thị giao dịch lên Server.
     */
    private void processTransaction(RequestType type, String actionName) {
        String amountStr = txtAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập số tiền!");
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền phải lớn hơn 0!");
                return;
            }

            new Thread(() -> {
                try {
                    Request req = new Request(type, amount);
                    Response res = ClientSocketManager.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        if (res.getStatus() == ResponseStatus.SUCCESS) {
                            txtAmount.clear();
                            if (MainController.getInstance() != null) {
                                MainController.getInstance().refreshBalanceFromServer();
                            }
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", actionName + " thành công!");
                            if (MainController.getInstance() != null) {
                                MainController.getInstance().loadPage("account.fxml");
                            }
                        } else {
                            showAlert(Alert.AlertType.WARNING, "Lỗi", res.getMessage() != null ? res.getMessage() : "Giao dịch thất bại");
                        }
                    });
                } catch (Exception e) {
                    // Đăng ký ngoại lệ xử lý kết nối lỗi giao dịch tài chính vào bộ Logger tập tin hệ thống
                    logger.log(Level.SEVERE, "Gặp sự cố lỗi kết nối mạng trong quá trình truyền phát chỉ thị giao dịch tài chính: ", e);
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền không hợp lệ!");
        }
    }

    // =========================================================================
    // PHƯƠNG THỨC TRỢ GIÚP GIAO DIỆN (HELPER METHOD)
    // =========================================================================

    /**
     * Đóng gói khởi tạo và hiển thị cấu trúc hộp thoại Pop-up (Modal Alert Window) tùy biến linh hoạt theo chỉ thị tham số đầu vào.
     */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}