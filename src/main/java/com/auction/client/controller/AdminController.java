package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.AlertUtils;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller trung tâm quản lý giao diện Admin Dashboard.
 */
public class AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

    // QUẢN LÝ NGƯỜI DÙNG FXML
    @FXML
    private TableView<UserResponseDTO> tableUsers;
    @FXML
    private TableColumn<UserResponseDTO, Boolean> colSelect;
    @FXML
    private TableColumn<UserResponseDTO, String> colUserId;
    @FXML
    private TableColumn<UserResponseDTO, String> colUserUsername;
    @FXML
    private TableColumn<UserResponseDTO, String> colUserFullName;
    @FXML
    private TableColumn<UserResponseDTO, String> colUserEmail;
    @FXML
    private TableColumn<UserResponseDTO, Object> colUserRole;
    @FXML
    private TableColumn<UserResponseDTO, Double> colUserBalance;
    @FXML
    private TableColumn<UserResponseDTO, Boolean> colUserStatus;

    @FXML
    private Label statTotalUsers;
    @FXML
    private Label statActiveUsers;
    @FXML
    private Label statLockedUsers;
    @FXML
    private Label statAdminUsers;

    @FXML
    private HBox pageButtonBox;
    @FXML
    private Label lblPageInfo;
    @FXML
    private Button btnFirstPage;
    @FXML
    private Button btnPrevPage;
    @FXML
    private Button btnNextPage;
    @FXML
    private Button btnLastPage;

    private static final int PAGE_SIZE = 15;
    private int currentPage = 0;
    private final List<UserResponseDTO> allUsersList = new ArrayList<>();
    private final ObservableList<UserResponseDTO> usersList = FXCollections.observableArrayList();
    private final Map<String, BooleanProperty> selectedUsersMap = new HashMap<>();

    // QUẢN LÝ PHIÊN ĐẤU GIÁ FXML
    @FXML
    private TableView<AuctionSummaryDTO> tableAuctions;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colAucProduct;
    @FXML
    private TableColumn<AuctionSummaryDTO, Double> colAucPrice;
    @FXML
    private TableColumn<AuctionSummaryDTO, Integer> colAucViews;
    @FXML
    private TableColumn<AuctionSummaryDTO, Integer> colAucBids;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colAucWinner;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colAucSeller;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colAucEnd;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colAucStatus;
    @FXML
    private TableColumn<AuctionSummaryDTO, Void> colAucAction;

    @FXML
    private Label aucStatTotal;
    @FXML
    private Label aucStatRunning;
    @FXML
    private Label aucStatOpen;
    @FXML
    private Label aucStatFinished;
    @FXML
    private Label aucStatPaid;
    @FXML
    private Label aucStatCanceled;

    @FXML
    private HBox aucPageButtonBox;
    @FXML
    private Label aucLblPageInfo;
    @FXML
    private Button aucBtnFirst;
    @FXML
    private Button aucBtnPrev;
    @FXML
    private Button aucBtnNext;
    @FXML
    private Button aucBtnLast;

    private static final int AUC_PAGE_SIZE = 5;
    private int aucCurrentPage = 0;
    private final List<AuctionSummaryDTO> allAuctionsList = new ArrayList<>();
    private final ObservableList<AuctionSummaryDTO> auctionsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tableUsers != null) {
            setupUsersTable();
            loadUsers();
        }
        if (tableAuctions != null) {
            setupAuctionsTable();
            loadAuctions();
        }
    }

    /**
     * Cấu hình TableView người dùng.
     */
    private void setupUsersTable() {
        if (colSelect != null) {
            colSelect.setCellValueFactory(cellData -> {
                String id = cellData.getValue().getId();
                selectedUsersMap.putIfAbsent(id, new SimpleBooleanProperty(false));
                return selectedUsersMap.get(id);
            });
            colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        }

        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserRole.setCellFactory(_ -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(Object roleObj, boolean empty) {
                super.updateItem(roleObj, empty);
                if (empty || roleObj == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String role = roleObj.toString();
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        badge.setText("QUẢN TRỊ VIÊN");
                        badge.getStyleClass().setAll("role-badge-admin");
                    } else {
                        badge.setText("THÀNH VIÊN");
                        badge.getStyleClass().setAll("role-badge-user");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colUserBalance.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null)
                    setText(null);
                else
                    setText(String.format("%,.0fđ", balance));
            }
        });

        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        colUserStatus.setCellFactory(_ -> new TableCell<>() {
            private final Label lbl = new Label();

            @Override
            protected void updateItem(Boolean isActive, boolean empty) {
                super.updateItem(isActive, empty);
                if (empty || isActive == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lbl.setText(isActive ? "✅ Hoạt động" : "🔒 Đã khóa");
                    lbl.getStyleClass().setAll(isActive ? "status-lbl-active" : "status-lbl-locked");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        tableUsers.setItems(usersList);
        tableUsers.setEditable(true);
    }

    /**
     * Cấu hình TableView phiên đấu giá.
     */
    private void setupAuctionsTable() {
        colAucProduct.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAucProduct.setCellFactory(_ -> new TableCell<>() {
            private final Label nameLbl = new Label();
            private final Label idLbl = new Label();
            private final Label typeLbl = new Label();
            private final HBox infoBox = new HBox(6);
            private final VBox box = new VBox(4);

            {
                nameLbl.getStyleClass().add("prod-title-lbl");
                idLbl.getStyleClass().add("prod-id-lbl");
                infoBox.getChildren().addAll(typeLbl, idLbl);
                box.getChildren().addAll(nameLbl, infoBox);
            }

            @Override
            protected void updateItem(String itemName, boolean empty) {
                super.updateItem(itemName, empty);
                if (empty || itemName == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        AuctionSummaryDTO dto = getTableView().getItems().get(index);
                        nameLbl.setText(itemName);
                        idLbl.setText("#" + dto.getAuctionId());

                        String type = dto.getItemType();
                        typeLbl.getStyleClass().clear();
                        if ("ELECTRONICS".equalsIgnoreCase(type)) {
                            typeLbl.setText("Điện tử");
                            typeLbl.getStyleClass().add("prod-type-electronics");
                        } else if ("ART".equalsIgnoreCase(type)) {
                            typeLbl.setText("Nghệ thuật");
                            typeLbl.getStyleClass().add("prod-type-art");
                        } else if ("VEHICLE".equalsIgnoreCase(type)) {
                            typeLbl.setText("Phương tiện");
                            typeLbl.getStyleClass().add("prod-type-vehicle");
                        } else {
                            typeLbl.setText(type);
                            typeLbl.getStyleClass().add("prod-type-default");
                        }

                        setGraphic(box);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                }
            }
        });

        colAucPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAucPrice.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    getStyleClass().remove("price-highlight");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    if (!getStyleClass().contains("price-highlight")) {
                        getStyleClass().add("price-highlight");
                    }
                }
            }
        });

        colAucBids.setCellValueFactory(new PropertyValueFactory<>("bidCount"));
        colAucBids.setStyle("-fx-alignment: CENTER;");

        colAucViews.setCellValueFactory(cellData -> {
            int bids = cellData.getValue().getBidCount();
            int hash = cellData.getValue().getAuctionId() != null
                    ? Math.abs(cellData.getValue().getAuctionId().hashCode())
                    : 0;
            int views = bids == 0 ? (hash % 5) : bids * 3 + (hash % 15);
            return new javafx.beans.property.SimpleIntegerProperty(views).asObject();
        });
        colAucViews.setStyle("-fx-alignment: CENTER; -fx-text-fill: #64748b;");

        colAucWinner.setCellValueFactory(new PropertyValueFactory<>("currentWinnerId"));
        colAucWinner.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String winner, boolean empty) {
                super.updateItem(winner, empty);
                if (empty) {
                    setText(null);
                    getStyleClass().removeAll("winner-undefined", "winner-defined");
                } else if (winner == null || winner.isEmpty()) {
                    setText("Chưa có");
                    getStyleClass().remove("winner-defined");
                    if (!getStyleClass().contains("winner-undefined")) {
                        getStyleClass().add("winner-undefined");
                    }
                } else {
                    setText(winner);
                    getStyleClass().remove("winner-undefined");
                    if (!getStyleClass().contains("winner-defined")) {
                        getStyleClass().add("winner-defined");
                    }
                }
            }
        });

        colAucSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colAucSeller.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String seller, boolean empty) {
                super.updateItem(seller, empty);
                if (empty) {
                    setText(null);
                    getStyleClass().removeAll("seller-undefined", "seller-defined");
                } else if (seller == null || seller.isEmpty()) {
                    setText("Không rõ");
                    getStyleClass().remove("seller-defined");
                    if (!getStyleClass().contains("seller-undefined")) {
                        getStyleClass().add("seller-undefined");
                    }
                } else {
                    setText(seller);
                    getStyleClass().remove("seller-undefined");
                    if (!getStyleClass().contains("seller-defined")) {
                        getStyleClass().add("seller-defined");
                    }
                }
            }
        });

        colAucEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colAucEnd.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String end, boolean empty) {
                super.updateItem(end, empty);
                if (empty || end == null) {
                    setText(null);
                    getStyleClass().remove("time-lbl");
                } else {
                    String[] parts = end.split(" ");
                    if (parts.length == 2) {
                        setText(parts[1] + "\n" + parts[0]);
                    } else {
                        setText(end);
                    }
                    if (!getStyleClass().contains("time-lbl")) {
                        getStyleClass().add("time-lbl");
                    }
                }
            }
        });

        colAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAucStatus.setCellFactory(_ -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.getStyleClass().clear();
                    badge.setStyle(null);
                    if ("RUNNING".equalsIgnoreCase(status)) {
                        badge.setText("Đang diễn ra");
                        badge.getStyleClass().add("auc-badge-running");
                    } else if ("OPEN".equalsIgnoreCase(status)) {
                        badge.setText("Sắp diễn ra");
                        badge.getStyleClass().add("auc-badge-open");
                    } else if ("FINISHED".equalsIgnoreCase(status)) {
                        badge.setText("Đã kết thúc");
                        badge.getStyleClass().add("auc-badge-finished");
                    } else if ("PAID".equalsIgnoreCase(status)) {
                        badge.setText("Đã thanh toán");
                        badge.getStyleClass().add("auc-badge-paid");
                    } else if ("CANCELED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                        badge.setText("Đã hủy");
                        badge.getStyleClass().add("auc-badge-canceled");
                    } else {
                        badge.setText(status);
                        badge.setStyle("-fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        if (colAucAction != null) {
            colAucAction.setCellFactory(_ -> new TableCell<>() {
                private final Button btnView = new Button("👁 Xem");
                private final Button btnClose = new Button("Đóng");
                private final Button btnCancel = new Button("Hủy");
                private final HBox box = new HBox(6);

                {
                    btnView.getStyleClass().add("btn-action-view");
                    btnClose.getStyleClass().add("btn-action-close");
                    btnCancel.getStyleClass().add("btn-action-cancel");
                    box.setAlignment(Pos.CENTER);
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            AuctionSummaryDTO row = getTableView().getItems().get(index);

                            btnView.setOnAction(
                                    _ -> MainController.getInstance().openAuctionDetail(row.getAuctionId()));
                            btnClose.setOnAction(_ -> forceCloseAuction(row.getAuctionId()));
                            btnCancel.setOnAction(_ -> cancelAuctionAdmin(row.getAuctionId()));

                            box.getChildren().clear();
                            String status = row.getStatus();
                            if ("RUNNING".equalsIgnoreCase(status) || "OPEN".equalsIgnoreCase(status)) {
                                box.getChildren().addAll(btnView, btnClose, btnCancel);
                            } else if ("FINISHED".equalsIgnoreCase(status)) {
                                box.getChildren().addAll(btnView, btnCancel);
                            } else {
                                box.getChildren().add(btnView);
                            }

                            setGraphic(box);
                            setText(null);
                        } else {
                            setGraphic(null);
                            setText(null);
                        }
                    }
                }
            });
        }

        tableAuctions.setFixedCellSize(60);
        tableAuctions.setItems(auctionsList);
    }

    /**
     * Tải danh sách người dùng từ server.
     */
    private void loadUsers() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_USERS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                    UserResponseDTO[] arr = response.getPayloadAs(UserResponseDTO[].class);
                    Platform.runLater(() -> {
                        allUsersList.clear();
                        if (arr != null)
                            Collections.addAll(allUsersList, arr);
                        currentPage = 0;
                        updateStats();
                        renderPage();
                    });
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối",
                            "Không thể tải danh sách người dùng từ máy chủ."));
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi mạng khi thực thi yêu cầu GET_ALL_USERS", e);
                Platform.runLater(
                        () -> AlertUtils.showError("Lỗi kết nối", "Gặp sự cố lỗi mạng khi tải danh sách người dùng."));
            }
        });
    }

    /**
     * Cập nhật nhãn thống kê người dùng.
     */
    private void updateStats() {
        int total = allUsersList.size();
        long active = allUsersList.stream().filter(UserResponseDTO::isActive).count();
        long locked = total - active;
        long admins = allUsersList.stream()
                .filter(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().toString()))
                .count();
        if (statTotalUsers != null)
            statTotalUsers.setText(String.valueOf(total));
        if (statActiveUsers != null)
            statActiveUsers.setText(String.valueOf(active));
        if (statLockedUsers != null)
            statLockedUsers.setText(String.valueOf(locked));
        if (statAdminUsers != null)
            statAdminUsers.setText(String.valueOf(admins));
    }

    /**
     * Hiển thị trang người dùng hiện hành.
     */
    private void renderPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE));
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, allUsersList.size());

        usersList.clear();
        selectedUsersMap.clear();
        usersList.addAll(allUsersList.subList(from, to));

        if (lblPageInfo != null)
            lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages
                    + "  (" + allUsersList.size() + " người dùng)");

        if (btnFirstPage != null)
            btnFirstPage.setDisable(currentPage == 0);
        if (btnPrevPage != null)
            btnPrevPage.setDisable(currentPage == 0);
        if (btnNextPage != null)
            btnNextPage.setDisable(currentPage >= totalPages - 1);
        if (btnLastPage != null)
            btnLastPage.setDisable(currentPage >= totalPages - 1);

        if (pageButtonBox != null) {
            pageButtonBox.getChildren().clear();
            int maxBtn = 5;
            int startP = Math.max(0, currentPage - maxBtn / 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn)
                startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                Button btn = new Button(String.valueOf(p + 1));
                btn.getStyleClass().setAll("button", p == currentPage ? "page-btn-active" : "page-btn-normal");
                btn.setOnAction(_ -> {
                    currentPage = pg;
                    renderPage();
                });
                pageButtonBox.getChildren().add(btn);
            }
        }
    }

    @FXML
    public void goFirstPage() {
        currentPage = 0;
        renderPage();
    }

    @FXML
    public void goPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    public void goNextPage() {
        int totalPages = (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderPage();
        }
    }

    @FXML
    public void goLastPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE));
        currentPage = totalPages - 1;
        renderPage();
    }

    /**
     * Tải danh sách phiên đấu giá từ server.
     */
    private void loadAuctions() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                    AuctionSummaryDTO[] arr = response.getPayloadAs(AuctionSummaryDTO[].class);
                    Platform.runLater(() -> {
                        allAuctionsList.clear();
                        if (arr != null)
                            Collections.addAll(allAuctionsList, arr);
                        aucCurrentPage = 0;
                        updateAucStats();
                        renderAucPage();
                    });
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối",
                            "Không thể tải danh sách phiên đấu giá từ máy chủ."));
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi mạng khi thực thi yêu cầu GET_ALL_AUCTIONS", e);
                Platform.runLater(
                        () -> AlertUtils.showError("Lỗi kết nối", "Gặp sự cố lỗi mạng khi tải danh sách đấu giá."));
            }
        });
    }

    /**
     * Cập nhật nhãn thống kê phiên đấu giá.
     */
    private void updateAucStats() {
        long total = allAuctionsList.size();
        long running = allAuctionsList.stream().filter(a -> "RUNNING".equalsIgnoreCase(a.getStatus())).count();
        long open = allAuctionsList.stream().filter(a -> "OPEN".equalsIgnoreCase(a.getStatus())).count();
        long finished = allAuctionsList.stream().filter(a -> "FINISHED".equalsIgnoreCase(a.getStatus())).count();
        long paid = allAuctionsList.stream().filter(a -> "PAID".equalsIgnoreCase(a.getStatus())).count();
        long canceled = allAuctionsList.stream().filter(a -> "CANCELED".equalsIgnoreCase(a.getStatus())).count();
        if (aucStatTotal != null)
            aucStatTotal.setText(String.valueOf(total));
        if (aucStatRunning != null)
            aucStatRunning.setText(String.valueOf(running));
        if (aucStatOpen != null)
            aucStatOpen.setText(String.valueOf(open));
        if (aucStatFinished != null)
            aucStatFinished.setText(String.valueOf(finished));
        if (aucStatPaid != null)
            aucStatPaid.setText(String.valueOf(paid));
        if (aucStatCanceled != null)
            aucStatCanceled.setText(String.valueOf(canceled));
    }

    /**
     * Hiển thị trang phiên đấu giá hiện hành.
     */
    private void renderAucPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE));
        int from = aucCurrentPage * AUC_PAGE_SIZE;
        int to = Math.min(from + AUC_PAGE_SIZE, allAuctionsList.size());

        auctionsList.clear();
        auctionsList.addAll(allAuctionsList.subList(from, to));

        if (aucLblPageInfo != null)
            aucLblPageInfo.setText("Trang " + (aucCurrentPage + 1) + " / " + totalPages
                    + "  (" + allAuctionsList.size() + " phiên)");

        if (aucBtnFirst != null)
            aucBtnFirst.setDisable(aucCurrentPage == 0);
        if (aucBtnPrev != null)
            aucBtnPrev.setDisable(aucCurrentPage == 0);
        if (aucBtnNext != null)
            aucBtnNext.setDisable(aucCurrentPage >= totalPages - 1);
        if (aucBtnLast != null)
            aucBtnLast.setDisable(aucCurrentPage >= totalPages - 1);

        if (aucPageButtonBox != null) {
            aucPageButtonBox.getChildren().clear();
            int maxBtn = 5;
            int startP = Math.max(0, aucCurrentPage - maxBtn / 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn)
                startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                Button btn = new Button(String.valueOf(p + 1));
                btn.getStyleClass().setAll("button", p == aucCurrentPage ? "page-btn-active" : "page-btn-normal");
                btn.setOnAction(_ -> {
                    aucCurrentPage = pg;
                    renderAucPage();
                });
                aucPageButtonBox.getChildren().add(btn);
            }
        }
    }

    @FXML
    public void aucGoFirstPage() {
        aucCurrentPage = 0;
        renderAucPage();
    }

    @FXML
    public void aucGoPrevPage() {
        if (aucCurrentPage > 0) {
            aucCurrentPage--;
            renderAucPage();
        }
    }

    @FXML
    public void aucGoNextPage() {
        int totalPages = (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE);
        if (aucCurrentPage < totalPages - 1) {
            aucCurrentPage++;
            renderAucPage();
        }
    }

    @FXML
    public void aucGoLastPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE));
        aucCurrentPage = totalPages - 1;
        renderAucPage();
    }

    /**
     * Khóa tài khoản các người dùng đã chọn.
     */
    @FXML
    void handleLockUser() {
        List<UserResponseDTO> selectedUsers = getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng tick chọn ít nhất một người dùng để khóa!");
            return;
        }

        ClientSocketManager.getInstance().execute(() -> {
            try {
                boolean allSuccess = true;
                String firstError = null;
                for (UserResponseDTO selected : selectedUsers) {
                    Request request = new Request(RequestType.LOCK_USER, selected.getId());
                    Response res = ClientSocketManager.getInstance().sendRequest(request);
                    if (res == null || res.getStatus() != ResponseStatus.SUCCESS) {
                        allSuccess = false;
                        if (res != null && res.getMessage() != null) {
                            firstError = res.getMessage();
                        }
                    }
                }
                final boolean success = allSuccess;
                final String errorMsg = firstError;
                Platform.runLater(() -> {
                    if (success) {
                        AlertUtils.showInfo("Thành công", "Đã khóa " + selectedUsers.size() + " người dùng được chọn.");
                    } else {
                        AlertUtils.showError("Lỗi",
                                errorMsg != null ? errorMsg : "Một số hoặc toàn bộ yêu cầu khóa tài khoản thất bại.");
                    }
                    selectedUsersMap.clear();
                    loadUsers();
                });
            } catch (Exception e) {
                LOGGER.error("Gặp lỗi kết nối khi truyền chỉ thị khóa tài khoản người dùng", e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    /**
     * Mở khóa tài khoản các người dùng đã chọn.
     */
    @FXML
    void handleUnlockUser() {
        List<UserResponseDTO> selectedUsers = getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng tick chọn ít nhất một người dùng để mở khóa!");
            return;
        }

        ClientSocketManager.getInstance().execute(() -> {
            try {
                boolean allSuccess = true;
                String firstError = null;
                for (UserResponseDTO selected : selectedUsers) {
                    Request request = new Request(RequestType.UNLOCK_USER, selected.getId());
                    Response res = ClientSocketManager.getInstance().sendRequest(request);
                    if (res == null || res.getStatus() != ResponseStatus.SUCCESS) {
                        allSuccess = false;
                        if (res != null && res.getMessage() != null) {
                            firstError = res.getMessage();
                        }
                    }
                }
                final boolean success = allSuccess;
                final String errorMsg = firstError;
                Platform.runLater(() -> {
                    if (success) {
                        AlertUtils.showInfo("Thành công",
                                "Đã mở khóa " + selectedUsers.size() + " người dùng được chọn.");
                    } else {
                        AlertUtils.showError("Lỗi", errorMsg != null ? errorMsg
                                : "Một số hoặc toàn bộ yêu cầu mở khóa tài khoản thất bại.");
                    }
                    selectedUsersMap.clear();
                    loadUsers();
                });
            } catch (Exception e) {
                LOGGER.error("Gặp lỗi kết nối khi truyền chỉ thị mở khóa tài khoản người dùng", e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    private List<UserResponseDTO> getSelectedUsers() {
        List<UserResponseDTO> list = new ArrayList<>();
        for (UserResponseDTO user : allUsersList) {
            if (selectedUsersMap.containsKey(user.getId()) && selectedUsersMap.get(user.getId()).get()) {
                list.add(user);
            }
        }
        if (list.isEmpty()) {
            UserResponseDTO selectedRow = tableUsers.getSelectionModel().getSelectedItem();
            if (selectedRow != null) {
                list.add(selectedRow);
            }
        }
        return list;
    }

    @FXML
    void handleForceClose() {
        AuctionSummaryDTO selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Lỗi", "Vui lòng chọn một phiên đấu giá để đóng!");
            return;
        }
        forceCloseAuction(selected.getAuctionId());
    }

    private void forceCloseAuction(String auctionId) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.CLOSE_AUCTION, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thành công", "Đã đóng phiên đấu giá: " + auctionId);
                        loadAuctions();
                    } else {
                        AlertUtils.showError("Lỗi",
                                (response != null && response.getMessage() != null) ? response.getMessage()
                                        : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng khi cưỡng chế đóng phiên đấu giá: {}", auctionId, e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    private void cancelAuctionAdmin(String auctionId) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.ADMIN_CANCEL_AUCTION, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thành công", "Đã hủy phiên đấu giá: " + auctionId);
                        loadAuctions();
                    } else {
                        AlertUtils.showError("Lỗi",
                                (response != null && response.getMessage() != null) ? response.getMessage()
                                        : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng khi admin yêu cầu hủy phiên đấu giá: {}", auctionId, e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }
}