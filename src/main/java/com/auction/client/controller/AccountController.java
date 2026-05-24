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
 * Controller quản lý thông tin cá nhân và lịch sử đấu giá của người dùng.
 */
public class AccountController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

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

    @FXML private TableView<MyBidHistoryDTO> tableHistory;
    @FXML private TableColumn<MyBidHistoryDTO, String> colAuctionId;
    @FXML private TableColumn<MyBidHistoryDTO, String> colItemName;
    @FXML private TableColumn<MyBidHistoryDTO, Double> colPrice;
    @FXML private TableColumn<MyBidHistoryDTO, LocalDateTime> colTime;
    @FXML private TableColumn<MyBidHistoryDTO, String> colResult;

    @FXML private HBox histPageBox;
    @FXML private Label histPageInfo;
    @FXML private Button histBtnFirst;
    @FXML private Button histBtnPrev;
    @FXML private Button histBtnNext;
    @FXML private Button histBtnLast;

    private static final int HIST_PAGE_SIZE = 5;
    private int histCurrentPage = 0;
    private final List<MyBidHistoryDTO> allHistory = new ArrayList<>();
    private final ObservableList<MyBidHistoryDTO> bidHistory = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadUserProfile();
        loadBidHistory();
    }

    /**
     * Cấu hình TableView và định dạng các cột hiển thị.
     */
    private void setupTable() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colItemName.setCellFactory(_ -> new TableCell<>() {
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

        colPrice.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colPrice.setCellFactory(_ -> new TableCell<>() {
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

        colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colTime.setCellFactory(_ -> new TableCell<>() {
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

        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colResult.setCellFactory(_ -> new TableCell<>() {
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
        tableHistory.setFixedCellSize(48);
    }

    /**
     * Tải thông tin người dùng từ session.
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

            if (user.getStoreName() != null && !user.getStoreName().isEmpty()) {
                boxStore.setVisible(true);
                boxStore.setManaged(true);
                lblStore.setText(user.getStoreName());
            }

            lblRating.setText(String.format("%.1f", user.getRating()));
            updateBalanceUI(user.getBalance());
        }
    }

    private void updateBalanceUI(double balance) {
        lblBalance.setText(String.format("%,.0f VNĐ", balance));
        if (MainController.getInstance() != null) {
            MainController.getInstance().updateHeaderBalance(balance);
        }
    }

    /**
     * Tải lịch sử thầu từ server.
     */
    private void loadBidHistory() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_MY_BID_HISTORY, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    List<MyBidHistoryDTO> history = res
                            .getPayloadAs(new com.google.gson.reflect.TypeToken<>() {
                            });

                    Platform.runLater(() -> {
                        allHistory.clear();
                        if (history != null)
                            allHistory.addAll(history);
                        histCurrentPage = 0;
                        renderHistPage();
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi tải lịch sử thầu từ Server", e);
            }
        });
    }

    /**
     * Phân trang hiển thị lịch sử đấu giá.
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

        if (histBtnFirst != null) histBtnFirst.setDisable(histCurrentPage == 0);
        if (histBtnPrev != null) histBtnPrev.setDisable(histCurrentPage == 0);
        if (histBtnNext != null) histBtnNext.setDisable(histCurrentPage >= totalPages - 1);
        if (histBtnLast != null) histBtnLast.setDisable(histCurrentPage >= totalPages - 1);

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
                btn.setOnAction(_ -> {
                    histCurrentPage = pg;
                    renderHistPage();
                });
                histPageBox.getChildren().add(btn);
            }
        }
    }

    @FXML
    public void histGoFirst() {
        histCurrentPage = 0;
        renderHistPage();
    }

    @FXML
    public void histGoPrev() {
        if (histCurrentPage > 0) {
            histCurrentPage--;
            renderHistPage();
        }
    }

    @FXML
    public void histGoNext() {
        int t = Math.max(1, (int) Math.ceil((double) allHistory.size() / HIST_PAGE_SIZE));
        if (histCurrentPage < t - 1) {
            histCurrentPage++;
            renderHistPage();
        }
    }

    @FXML
    public void histGoLast() {
        histCurrentPage = Math.max(0, (int) Math.ceil((double) allHistory.size() / HIST_PAGE_SIZE) - 1);
        renderHistPage();
    }

    /**
     * Mở dialog chỉnh sửa thông tin hồ sơ cá nhân.
     */
    @FXML
    void handleEditProfile() {
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

        okNode.addEventFilter(javafx.event.ActionEvent.ACTION, filterEvent -> {
            if (tfName.getText().trim().isEmpty()) {
                lblMsg.setText("Họ và tên không được để trống!");
                filterEvent.consume();
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
                                    LOGGER.error("Không thể cập nhật session người dùng", ex);
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
                    LOGGER.error("Lỗi cập nhật hồ sơ cá nhân", ex);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", ex.getMessage()));
                }
            });
        });
    }

    @FXML
    void handleDeposit() {
        processTransaction(RequestType.DEPOSIT, "Nạp tiền");
    }

    @FXML
    void handleWithdraw() {
        processTransaction(RequestType.WITHDRAW, "Rút tiền");
    }

    /**
     * Xử lý nạp/rút tiền tài khoản.
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
                                if (MainController.getInstance() != null) MainController.getInstance().loadPage("Account.fxml");
                            } else {
                                AlertUtils.showWarning("Lỗi", res.getMessage() != null ? res.getMessage() : "Giao dịch thất bại");
                            }
                        } else {
                            AlertUtils.showError("Lỗi kết nối", "Không nhận được phản hồi từ Server.");
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Lỗi giao dịch tài chính: " + actionName, e);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Lỗi", "Số tiền không hợp lệ!");
        }
    }
}