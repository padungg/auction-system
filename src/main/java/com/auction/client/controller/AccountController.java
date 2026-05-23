package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.MyBidHistoryDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <h2>AccountController</h2>
 * <p>
 * Controller chịu trách nhiệm điều khiển toàn bộ phân hệ Quản lý hồ sơ cá nhân (Account Profile)
 * và Lịch sử đấu giá trên giao diện Client ứng dụng đấu giá trực tuyến.
 * </p>
 *
 * <p><b>Chức năng cốt lõi:</b></p>
 * <ul>
 *   <li>Hiển thị chi tiết thông tin cá nhân và định dạng huy hiệu thành viên độc lập.</li>
 *   <li>Quản lý danh sách lịch sử tham gia đặt giá tích hợp thuật toán phân trang phía Client (Client-side Pagination).</li>
 *   <li>Khởi tạo biểu mẫu động (Dynamic Dialog Grid) hỗ trợ cập nhật thông tin bảo mật tài khoản.</li>
 *   <li>Xử lý đồng bộ hóa các giao dịch tài chính (Nạp/Rút tiền) thời gian thực thông qua kết nối Socket ngầm.</li>
 * </ul>
 *
 * @since 1.0
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.client.util.SessionManager
 */
public class AccountController {

    /**
     * Bộ ghi log tập trung (SLF4J Logger) dùng để giám sát và ghi vết (trace)
     * luồng thực thi, phục vụ công tác gỡ lỗi và quản trị vận hành hệ thống.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - HỒ SƠ CÁ NHÂN (PROFILE LABELS & INPUTS)
    // =========================================================================
    @FXML
    private Label lblAvatar;
    @FXML
    private Label lblName;
    @FXML
    private Label lblUsername;
    @FXML
    private Label lblEmail;
    @FXML
    private Label lblRole;
    @FXML
    private Label lblPhone;
    @FXML
    private Label lblAddress;
    @FXML
    private Label lblStore;
    @FXML
    private Label lblRating;
    @FXML
    private Label lblBalance;
    @FXML
    private HBox boxStore;
    @FXML
    private TextField txtAmount;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - BẢNG LỊCH SỬ ĐẶT GIÁ (BID HISTORY TABLE)
    // =========================================================================
    @FXML
    private TableView<MyBidHistoryDTO> tableHistory;
    @FXML
    private TableColumn<MyBidHistoryDTO, String> colAuctionId;
    @FXML
    private TableColumn<MyBidHistoryDTO, String> colItemName;
    @FXML
    private TableColumn<MyBidHistoryDTO, Double> colPrice;
    @FXML
    private TableColumn<MyBidHistoryDTO, LocalDateTime> colTime;
    @FXML
    private TableColumn<MyBidHistoryDTO, String> colResult;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - ĐIỀU KHIỂN PHÂN TRANG (PAGINATION CONTROLS)
    // =========================================================================
    @FXML
    private HBox histPageBox;
    @FXML
    private Label histPageInfo;
    @FXML
    private Button histBtnFirst;
    @FXML
    private Button histBtnPrev;
    @FXML
    private Button histBtnNext;
    @FXML
    private Button histBtnLast;

    // =========================================================================
    // BIẾN QUẢN LÝ DỮ LIỆU NỘI BỘ (INTERNAL DATA MANAGEMENTS)
    // =========================================================================
    /** Số lượng bản ghi tối đa hiển thị trên một trang lịch sử. */
    private static final int HIST_PAGE_SIZE = 5;

    /** Chỉ mục trang hiện tại trong cấu trúc phân trang (bắt đầu từ 0). */
    private int histCurrentPage = 0;

    /** Bộ nhớ đệm lưu trữ toàn bộ danh sách lịch sử đặt giá nhận về từ Server. */
    private List<MyBidHistoryDTO> allHistory = new ArrayList<>();

    /** Danh sách quan sát (ObservableList) liên kết trực tiếp để hiển thị dữ liệu lên TableView. */
    private ObservableList<MyBidHistoryDTO> bidHistory = FXCollections.observableArrayList();

    /**
     * Phương thức khởi tạo vòng đời của JavaFX View (Lifecycle Hook).
     * Được tự động kích hoạt sau khi file FXML liên kết được nạp thành công.
     * Thực hiện thiết lập cấu trúc bảng, kết xuất thông tin phiên hiện hành và kéo dữ liệu từ mạng.
     */
    @FXML
    public void initialize() {
        setupTable();
        loadUserProfile();
        loadBidHistory();
    }

    /**
     * Cấu hình cấu trúc TableView và định nghĩa quy tắc render dữ liệu (Cell Factory).
     * Áp dụng các định dạng hiển thị đặc thù cho tiền tệ (VNĐ), thời gian (HH:mm dd/MM/yyyy)
     * và phân cấp đồ họa CSS cho các trạng thái kết quả đấu giá dưới dạng Badge trực quan.
     */
    private void setupTable() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

        // Định cấu hình hiển thị cột tên sản phẩm và đính kèm class CSS chuyên biệt
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colItemName.setCellFactory(tc -> new TableCell<MyBidHistoryDTO, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    getStyleClass().remove("item-name-cell");
                } else {
                    setText(name);
                    if (!getStyleClass().contains("item-name-cell")) {
                        getStyleClass().add("item-name-cell");
                    }
                }
            }
        });

        // Định dạng cột hiển thị giá trị đặt: Thêm dấu phân cách phần nghìn và đơn vị tiền tệ
        colPrice.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colPrice.setCellFactory(tc -> new TableCell<MyBidHistoryDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    getStyleClass().remove("price-cell");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    if (!getStyleClass().contains("price-cell")) {
                        getStyleClass().add("price-cell");
                    }
                }
            }
        });

        // Định dạng hiển thị ngày giờ chuẩn hóa: Giờ:Phút Ngày/Tháng/Năm
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

        // Ánh xạ trạng thái kết quả sang các Badge đồ họa màu sắc tương ứng
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colResult.setCellFactory(tc -> new TableCell<MyBidHistoryDTO, String>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("badge-base");
            }
            @Override
            protected void updateItem(String result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                badge.getStyleClass().setAll("badge-base");

                switch (result) {
                    case "Thắng":
                        badge.setText("🏆 Thắng");
                        badge.getStyleClass().add("badge-win");
                        break;
                    case "Thất bại":
                        badge.setText("❌ Thất bại");
                        badge.getStyleClass().add("badge-lose");
                        break;
                    case "Đang đấu":
                        badge.setText("⚡ Đang đấu");
                        badge.getStyleClass().add("badge-active");
                        break;
                    default:
                        badge.setText(result);
                        badge.getStyleClass().add("badge-default");
                }

                setGraphic(badge);
                setAlignment(Pos.CENTER);
                setText(null);
            }
        });

        tableHistory.setItems(bidHistory);
        tableHistory.setFixedCellSize(48); // Khống chế chiều cao dòng cố định để tối ưu hóa hiệu năng render
    }

    /**
     * Tải và đồng bộ thông tin tài khoản người dùng hiện tại từ Session bộ nhớ đệm (Memory Cache).
     * Thực hiện gán dữ liệu động lên toàn bộ các thành phần hiển thị đầu cuối (UI Labels).
     */
    private void loadUserProfile() {
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName()
                    : user.getUsername();
            lblName.setText(name);
            lblAvatar.setText(name.substring(0, 1).toUpperCase());
            lblUsername.setText("@" + user.getUsername());
            lblEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa cập nhật");
            lblRole.setText(user.getRole() != null ? "Vai trò: " + user.getRole() : "MEMBER");
            lblPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Chưa cập nhật");
            lblAddress.setText(
                    user.getAddress() != null && !user.getAddress().isEmpty() ? user.getAddress() : "Chưa cập nhật");

            // Hiển thị và phân bổ không gian cho khối cửa hàng nếu tài khoản đăng ký là chủ shop
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
     * Cập nhật số dư tài khoản trên View cục bộ và đồng bộ hóa hiển thị lên Header của MainController.
     *
     * @param balance Số dư tài khoản mới cần cập nhật
     */
    private void updateBalanceUI(double balance) {
        lblBalance.setText(String.format("%,.0f VNĐ", balance));
        if (MainController.getInstance() != null) {
            MainController.getInstance().updateHeaderBalance(balance);
        }
    }

    /**
     * Kích hoạt luồng xử lý bất đồng bộ (Worker Thread) gửi yêu cầu mạng truy vấn
     * toàn bộ lịch sử đấu giá của người dùng hiện hành từ Server, sau đó đẩy dữ liệu về UI Thread.
     */
    private void loadBidHistory() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_MY_BID_HISTORY, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    List<MyBidHistoryDTO> history = res
                            .getPayloadAs(new com.google.gson.reflect.TypeToken<List<MyBidHistoryDTO>>() {
                            });

                    // Chuyển tiếp tiến trình cập nhật giao diện về JavaFX Application Thread tránh xung đột luồng
                    Platform.runLater(() -> {
                        allHistory.clear();
                        if (history != null)
                            allHistory.addAll(history);
                        histCurrentPage = 0;
                        renderHistPage();
                    });
                } else if (res == null) {
                    LOGGER.error("Mất kết nối mạng hoặc không nhận được phản hồi từ Server.");
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi xảy ra trong tiến trình tải danh sách lịch sử đặt giá từ Server", e);
            }
        });
    }

    /**
     * Thuật toán phân trang dữ liệu (Pagination Engine) tại thiết bị Client.
     * Tính toán lát cắt chỉ mục (Sub-list Slice) tương ứng với số trang hiện tại, kiểm soát
     * trạng thái kích hoạt của các nút điều hướng nhanh và sinh các nút chọn trang số động.
     */
    private void renderHistPage() {
        int total = allHistory.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / HIST_PAGE_SIZE));
        int from = histCurrentPage * HIST_PAGE_SIZE;
        int to = Math.min(from + HIST_PAGE_SIZE, total);

        bidHistory.clear();
        bidHistory.addAll(allHistory.subList(from, to));

        if (histPageInfo != null) {
            histPageInfo.setText("Trang " + (histCurrentPage + 1) + " / " + totalPages + "  (" + total + " lượt)");
        }

        // Bật/Tắt trạng thái điều hướng dựa trên biên chỉ mục trang
        if (histBtnFirst != null)
            histBtnFirst.setDisable(histCurrentPage == 0);
        if (histBtnPrev != null)
            histBtnPrev.setDisable(histCurrentPage == 0);
        if (histBtnNext != null)
            histBtnNext.setDisable(histCurrentPage >= totalPages - 1);
        if (histBtnLast != null)
            histBtnLast.setDisable(histCurrentPage >= totalPages - 1);

        // Khởi tạo thanh chỉ mục số trang động (Hiển thị tối đa dạng Slider 5 nút liền kề)
        if (histPageBox != null) {
            histPageBox.getChildren().clear();
            int maxBtn = 5, startP = Math.max(0, histCurrentPage - 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn)
                startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                Button btn = new Button(String.valueOf(p + 1));
                btn.getStyleClass().setAll("button", pg == histCurrentPage ? "page-btn-active" : "page-btn-normal");
                btn.setOnAction(e -> {
                    histCurrentPage = pg;
                    renderHistPage();
                });
                histPageBox.getChildren().add(btn);
            }
        }
    }

    /**
     * Điều hướng về trang lịch sử đầu tiên.
     * @param e Sự kiện kích hoạt Action từ giao diện.
     */
    @FXML
    public void histGoFirst(ActionEvent e) {
        histCurrentPage = 0;
        renderHistPage();
    }

    /**
     * Lùi lại một trang lịch sử (nếu có thể).
     * @param e Sự kiện kích hoạt Action từ giao diện.
     */
    @FXML
    public void histGoPrev(ActionEvent e) {
        if (histCurrentPage > 0) {
            histCurrentPage--;
            renderHistPage();
        }
    }

    /**
     * Tiến lên một trang lịch sử tiếp theo (nếu có thể).
     * @param e Sự kiện kích hoạt Action từ giao diện.
     */
    @FXML
    public void histGoNext(ActionEvent e) {
        int t = Math.max(1, (int) Math.ceil((double) allHistory.size() / HIST_PAGE_SIZE));
        if (histCurrentPage < t - 1) {
            histCurrentPage++;
            renderHistPage();
        }
    }

    /**
     * Điều hướng thẳng đến trang lịch sử cuối cùng.
     * @param e Sự kiện kích hoạt Action từ giao diện.
     */
    @FXML
    public void histGoLast(ActionEvent e) {
        histCurrentPage = Math.max(0, (int) Math.ceil((double) allHistory.size() / HIST_PAGE_SIZE) - 1);
        renderHistPage();
    }

    /**
     * Kích hoạt và hiển thị cửa sổ hộp thoại điều hướng (JavaFX Custom Dialog Layout)
     * cho phép chỉnh sửa thông tin hồ sơ bảo mật cá nhân của người dùng hiện hành.
     * Thực hiện kiểm tra dữ liệu đầu vào (Validation) trước khi đóng gói và gửi payload qua Socket.
     *
     * @param event Sự kiện nhấp nút "Chỉnh sửa hồ sơ" từ UI.
     */
    @FXML
    void handleEditProfile(ActionEvent event) {
        UserResponseDTO user = SessionManager.getInstance().getCurrentUser();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa hồ sơ");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Khởi tạo layout lưới phân bố các trường nhập liệu trên biểu mẫu popup
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 30, 20, 30));
        grid.setPrefWidth(380);

        TextField tfName = new TextField(user != null && user.getFullName() != null ? user.getFullName() : "");
        TextField tfPhone = new TextField(user != null && user.getPhone() != null ? user.getPhone() : "");
        TextField tfAddr = new TextField(user != null && user.getAddress() != null ? user.getAddress() : "");
        TextField tfStore = new TextField(user != null && user.getStoreName() != null ? user.getStoreName() : "");
        PasswordField tfPass = new PasswordField();
        tfPass.setPromptText("Mật khẩu mới (bỏ trống nếu không đổi)");

        Label lblMsg = new Label();
        lblMsg.getStyleClass().add("error-msg");

        tfName.getStyleClass().add("edit-profile-input");
        tfPhone.getStyleClass().add("edit-profile-input");
        tfAddr.getStyleClass().add("edit-profile-input");
        tfStore.getStyleClass().add("edit-profile-input");
        tfPass.getStyleClass().add("edit-profile-input");

        grid.add(new Label("Họ và tên *"), 0, 0);
        grid.add(tfName, 1, 0);
        grid.add(new Label("Số điện thoại"), 0, 1);
        grid.add(tfPhone, 1, 1);
        grid.add(new Label("Địa chỉ"), 0, 2);
        grid.add(tfAddr, 1, 2);
        grid.add(new Label("Tên cửa hàng"), 0, 3);
        grid.add(tfStore, 1, 3);
        grid.add(new Label("Đổi mật khẩu"), 0, 4);
        grid.add(tfPass, 1, 4);
        grid.add(lblMsg, 1, 5);

        dialog.getDialogPane().setContent(grid);

        javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.getStyleClass().add("edit-profile-save-btn");

        // Sử dụng EventFilter chặn hành vi đóng cửa sổ nếu kiểm tra dữ liệu bắt buộc bị lỗi
        okNode.addEventFilter(javafx.event.ActionEvent.ACTION, filterEvent -> {
            if (tfName.getText().trim().isEmpty()) {
                lblMsg.setText("Họ và tên không được để trống!");
                filterEvent.consume(); // Tiêu thụ sự kiện nhằm ngăn cản Dialog đóng lại
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != saveBtn) return;

            String fullName = tfName.getText().trim();
            String phone = tfPhone.getText().trim();
            String address = tfAddr.getText().trim();
            String storeName = tfStore.getText().trim();
            String password = tfPass.getText().trim();

            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("fullName", fullName);
            payload.put("phone", phone);
            payload.put("address", address);
            payload.put("storeName", storeName);
            if (!password.isEmpty()) payload.put("password", password);

            // Gửi dữ liệu cập nhật không đồng bộ thông qua kênh truyền Network
            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Request req = new Request(RequestType.UPDATE_PROFILE, payload);
                    Response res = ClientSocketManager.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        if (res != null) {
                            if (res.getStatus() == ResponseStatus.SUCCESS) {
                                try {
                                    UserResponseDTO updatedUser = res.getPayloadAs(UserResponseDTO.class);
                                    SessionManager.getInstance().setCurrentUser(updatedUser);
                                } catch (Exception ex) {
                                    LOGGER.error("Không thể cập nhật thông tin thực thể vào Session bộ nhớ đệm", ex);
                                }
                                AlertUtils.showInfo("Thành công", "Cập nhật hồ sơ thành công!");
                                if (MainController.getInstance() != null) MainController.getInstance().loadPage("Account.fxml");
                            } else {
                                AlertUtils.showWarning("Lỗi", res.getMessage() != null ? res.getMessage() : "Cập nhật thất bại!");
                            }
                        } else {
                            AlertUtils.showError("Lỗi kết nối", "Không nhận được phản hồi từ Server.");
                        }
                    });
                } catch (Exception ex) {
                    LOGGER.error("Gặp sự cố ngắt kết nối mạng", ex);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", ex.getMessage()));
                }
            });
        });
    }

    /**
     * Tiếp nhận lệnh nạp tiền từ giao diện UI.
     * @param event Sự kiện nhấp chuột kích hoạt xử lý.
     */
    @FXML
    void handleDeposit(ActionEvent event) {
        processTransaction(RequestType.DEPOSIT, "Nạp tiền");
    }

    /**
     * Tiếp nhận lệnh rút tiền từ giao diện UI.
     * @param event Sự kiện nhấp chuột kích hoạt xử lý.
     */
    @FXML
    void handleWithdraw(ActionEvent event) {
        processTransaction(RequestType.WITHDRAW, "Rút tiền");
    }

    /**
     * Xử lý giao dịch tài chính tập trung (Hợp nhất xử lý Nạp tiền / Rút tiền).
     * Tiến hành xác thực định dạng số thực nhập vào từ textfield trước khi truyền tải gói tin.
     *
     * @param type       Loại chỉ thị yêu cầu mạng (DEPOSIT hoặc WITHDRAW)
     * @param actionName Tên chuỗi hành động phục vụ hiển thị thông báo kết quả (Nạp tiền / Rút tiền)
     */
    private void processTransaction(RequestType type, String actionName) {
        String amountStr = txtAmount.getText().trim();
        if (amountStr.isEmpty()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập số tiền!");
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                AlertUtils.showWarning("Lỗi", "Số tiền phải lớn hơn 0!");
                return;
            }

            // Giao tiếp luồng IO luồng ngầm gửi lệnh giao dịch tài chính
            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Request req = new Request(type, amount);
                    Response res = ClientSocketManager.getInstance().sendRequest(req);

                    Platform.runLater(() -> {
                        if (res != null) {
                            if (res.getStatus() == ResponseStatus.SUCCESS) {
                                txtAmount.clear();
                                if (MainController.getInstance() != null) MainController.getInstance().refreshBalanceFromServer();
                                AlertUtils.showInfo("Thành công", actionName + " thành công!");
                                // Tải lại view hiện hành để làm mới toàn bộ trường thông tin số dư tài khoản
                                if (MainController.getInstance() != null) MainController.getInstance().loadPage("Account.fxml");
                            } else {
                                AlertUtils.showWarning("Lỗi", res.getMessage() != null ? res.getMessage() : "Giao dịch thất bại");
                            }
                        } else {
                            AlertUtils.showError("Lỗi kết nối", "Không nhận được phản hồi từ Server.");
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Lỗi giao dịch tài chính: {}", actionName, e);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Lỗi", "Số tiền không hợp lệ!");
        }
    }
}