package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý phân hệ Giao dịch, Hóa đơn và Tất toán tài chính phía Client.
 */
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    // THÀNH PHẦN CHUYỂN ĐỔI TAB FXML
    @FXML private Button btnTabPending;
    @FXML private Button btnTabHistory;

    // CÁC KHUNG GIAO DIỆN CHÍNH FXML
    @FXML private VBox panelPending;
    @FXML private VBox panelHistory;
    @FXML private VBox cardContainer;
    @FXML private Label lblNoPending;

    // BẢNG LỊCH SỬ GIAO DỊCH FXML
    @FXML private TableView<AuctionSummaryDTO> tableHistory;
    @FXML private TableColumn<AuctionSummaryDTO, String> colHistName;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colHistPrice;
    @FXML private TableColumn<AuctionSummaryDTO, String> colHistTime;
    @FXML private TableColumn<AuctionSummaryDTO, String> colHistStatus;

    // NÚT PHÂN TRANG GIAO DỊCH FXML
    @FXML private HBox histPageBox;
    @FXML private Label histPageInfo;
    @FXML private Button histBtnFirst;
    @FXML private Button histBtnPrev;
    @FXML private Button histBtnNext;
    @FXML private Button histBtnLast;

    private static final int HIST_PAGE_SIZE = 10;
    private int histCurrentPage = 0;

    private final java.util.List<AuctionSummaryDTO> allHistory = new java.util.ArrayList<>();
    private final ObservableList<AuctionSummaryDTO> historyList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupHistoryTable();
        loadPending();
        loadHistory();
    }

    /**
     * Cấu hình TableView lịch sử giao dịch.
     */
    private void setupHistoryTable() {
        colHistName.setCellValueFactory(new PropertyValueFactory<>("itemName"));

        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colHistPrice.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("pay-hist-price");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    setGraphic(null);
                    if (!getStyleClass().contains("pay-hist-price")) {
                        getStyleClass().add("pay-hist-price");
                    }
                }
            }
        });

        colHistTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        colHistStatus.setCellFactory(_ -> new TableCell<>() {
            private final Label lbl = new Label("✅ Đã thanh toán");
            {
                lbl.getStyleClass().add("pay-hist-status");
                setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        tableHistory.setItems(historyList);
        tableHistory.setFixedCellSize(50);
    }

    /**
     * Chuyển tab sang "Chờ thanh toán".
     */
    @FXML
    void switchTabPending() {
        btnTabPending.setStyle(null);
        btnTabHistory.setStyle(null);
        setTabActive(btnTabPending);
        setTabInactive(btnTabHistory);
        panelPending.setVisible(true);
        panelPending.setManaged(true);
        panelHistory.setVisible(false);
        panelHistory.setManaged(false);
        loadPending();
    }

    /**
     * Chuyển tab sang "Lịch sử thanh toán".
     */
    @FXML
    void switchTabHistory() {
        btnTabPending.setStyle(null);
        btnTabHistory.setStyle(null);
        setTabActive(btnTabHistory);
        setTabInactive(btnTabPending);
        panelHistory.setVisible(true);
        panelHistory.setManaged(true);
        panelPending.setVisible(false);
        panelPending.setManaged(false);
        loadHistory();
    }

    private void setTabActive(Button btn) {
        btn.getStyleClass().remove("pay-tab-inactive");
        if (!btn.getStyleClass().contains("pay-tab-active")) {
            btn.getStyleClass().add("pay-tab-active");
        }
    }

    private void setTabInactive(Button btn) {
        btn.getStyleClass().remove("pay-tab-active");
        if (!btn.getStyleClass().contains("pay-tab-inactive")) {
            btn.getStyleClass().add("pay-tab-inactive");
        }
    }

    /**
     * Tải danh sách hóa đơn chờ từ server.
     */
    private void loadPending() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_PENDING_PAYMENTS, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    AuctionSummaryDTO[] arr = res.getPayloadAs(AuctionSummaryDTO[].class);
                    Platform.runLater(() -> buildPendingCards(arr));
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", "Không thể lấy danh sách chờ thanh toán từ máy chủ!"));
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi tải chờ thanh toán", e);
            }
        });
    }

    /**
     * Dựng giao diện thẻ cho các phiên chờ tất toán.
     */
    private void buildPendingCards(AuctionSummaryDTO[] arr) {
        cardContainer.getChildren().clear();
        if (arr == null || arr.length == 0) {
            lblNoPending.setVisible(true);
            lblNoPending.setManaged(true);
            return;
        }
        lblNoPending.setVisible(false);
        lblNoPending.setManaged(false);

        UserResponseDTO currentUser = SessionManager.getInstance().getCurrentUser();
        double userBalance = currentUser != null ? currentUser.getBalance() : 0;

        for (AuctionSummaryDTO dto : arr) {
            cardContainer.getChildren().add(buildCard(dto, userBalance));
        }
    }

    /**
     * Thiết kế card hiển thị chi tiết hóa đơn chờ.
     */
    private HBox buildCard(AuctionSummaryDTO dto, double userBalance) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("pay-card");

        Label icon = new Label("🖥");
        icon.getStyleClass().add("pay-icon");
        if ("ART".equalsIgnoreCase(dto.getItemType())) {
            icon.setText("🎨");
        } else if ("VEHICLE".equalsIgnoreCase(dto.getItemType())) {
            icon.setText("🚗");
        }

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(dto.getItemName() != null ? dto.getItemName() : "Không rõ");
        nameLabel.getStyleClass().add("pay-name-label");

        String itemType = dto.getItemType();
        String displayType;
        String typeClass = "card-badge-open";
        if ("ELECTRONICS".equalsIgnoreCase(itemType)) {
            displayType = "Điện tử";
            typeClass = "card-badge-open";
        } else if ("ART".equalsIgnoreCase(itemType)) {
            displayType = "Nghệ thuật";
            typeClass = "card-badge-cancelled";
        } else if ("VEHICLE".equalsIgnoreCase(itemType)) {
            displayType = "Phương tiện";
            typeClass = "card-badge-paid";
        } else {
            displayType = itemType != null ? itemType : "";
        }
        Label typeBadge = new Label(displayType);
        typeBadge.getStyleClass().add(typeClass);

        HBox metaBox = new HBox(8);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.getChildren().add(typeBadge);

        Label condLabel = new Label("🟡 Tình trạng: Như mới");
        condLabel.getStyleClass().add("pay-cond-label");
        metaBox.getChildren().add(condLabel);

        Label endLabel = new Label("⏱ Kết thúc: " + (dto.getEndTime() != null ? dto.getEndTime() : "N/A"));
        endLabel.getStyleClass().add("pay-end-label");

        String deadline = "3 ngày sau khi kết thúc";
        HBox deadlineBox = new HBox(6);
        deadlineBox.setAlignment(Pos.CENTER_LEFT);
        deadlineBox.getStyleClass().add("pay-deadline-box");
        Label deadlineIcon = new Label("⏰");
        Label deadlineLabel = new Label("Hạn thanh toán: " + deadline);
        deadlineLabel.getStyleClass().add("pay-deadline-label");
        deadlineBox.getChildren().addAll(deadlineIcon, deadlineLabel);

        Label auctionIdLabel = new Label("Mã phiên: #" + dto.getAuctionId());
        auctionIdLabel.getStyleClass().add("pay-auction-id-label");

        info.getChildren().addAll(nameLabel, metaBox, endLabel, deadlineBox, auctionIdLabel);

        double price = dto.getCurrentPrice();
        double fee = price * 0.02;
        double total = price + fee;
        boolean canPay = userBalance >= total;

        VBox priceBlock = new VBox(4);
        priceBlock.setAlignment(Pos.TOP_RIGHT);
        priceBlock.setMinWidth(200);

        Label priceCaption = new Label("GIÁ TRÚNG");
        priceCaption.getStyleClass().add("pay-price-caption");

        Label priceLabel = new Label(String.format("%,.0f VNĐ", price));
        priceLabel.getStyleClass().add("pay-price-label");

        Label feeLabel = new Label(String.format("Phí DV (2%%): %,.0f VNĐ", fee));
        feeLabel.getStyleClass().add("pay-fee-label");

        Label totalLabel = new Label(String.format("Tổng: %,.0f VNĐ", total));
        totalLabel.getStyleClass().add("pay-total-label");

        Button btnView = new Button("🔍 Xem sản phẩm");
        btnView.getStyleClass().add("pay-btn-view");
        btnView.setOnAction(_ -> MainController.getInstance().openAuctionDetail(dto.getAuctionId()));

        Button btnPay = new Button("💳 Nạp tiền & Thanh toán");
        if (canPay) {
            btnPay.setText("💳 Thanh toán ngay");
            btnPay.getStyleClass().add("pay-btn-pay-now");
        } else {
            btnPay.getStyleClass().add("pay-btn-pay-insufficient");
        }
        btnPay.setOnAction(_ -> handlePayForAuction(dto));

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(btnView, btnPay);

        if (!canPay) {
            double needed = total - userBalance;
            Label balWarning = new Label(String.format("⚠ Số dư không đủ — cần thêm %,.0f VNĐ", needed));
            balWarning.getStyleClass().add("pay-bal-warning");
            priceBlock.getChildren().addAll(priceCaption, priceLabel, feeLabel, totalLabel, btnRow, balWarning);
        } else {
            priceBlock.getChildren().addAll(priceCaption, priceLabel, feeLabel, totalLabel, btnRow);
        }

        card.getChildren().addAll(icon, info, priceBlock);
        return card;
    }

    /**
     * Xử lý thanh toán phiên đấu giá.
     */
    private void handlePayForAuction(AuctionSummaryDTO dto) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.PAY_AUCTION, dto.getAuctionId());
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thanh toán thành công! 🎉",
                                "Bạn đã thanh toán thành công cho sản phẩm: " + dto.getItemName());
                        loadPending();
                        loadHistory();
                        if (MainController.getInstance() != null) {
                            MainController.getInstance().refreshBalanceFromServer();
                        }
                    } else {
                        String errorMsg = (res != null) ? res.getMessage() : "Không thể kết nối máy chủ";
                        AlertUtils.showError("Lỗi thanh toán", errorMsg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    /**
     * Tải lịch sử tất toán thành công từ server.
     */
    private void loadHistory() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_PAYMENT_HISTORY, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    AuctionSummaryDTO[] arr = res.getPayloadAs(AuctionSummaryDTO[].class);
                    Platform.runLater(() -> {
                        allHistory.clear();
                        if (arr != null) {
                            java.util.Collections.addAll(allHistory, arr);
                        }
                        histCurrentPage = 0;
                        renderHistPage();
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi tải lịch sử thanh toán", e);
            }
        });
    }

    /**
     * Thuật toán phân trang lịch sử giao dịch.
     */
    private void renderHistPage() {
        int total = allHistory.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / HIST_PAGE_SIZE));
        int from = histCurrentPage * HIST_PAGE_SIZE;
        int to = Math.min(from + HIST_PAGE_SIZE, total);

        historyList.clear();
        historyList.addAll(allHistory.subList(from, to));

        if (histPageInfo != null) {
            histPageInfo.setText("Trang " + (histCurrentPage + 1) + " / " + totalPages
                    + "  (" + total + " giao dịch)");
        }

        if (histBtnFirst != null) {
            histBtnFirst.setDisable(histCurrentPage == 0);
        }
        if (histBtnPrev != null) {
            histBtnPrev.setDisable(histCurrentPage == 0);
        }
        if (histBtnNext != null) {
            histBtnNext.setDisable(histCurrentPage >= totalPages - 1);
        }
        if (histBtnLast != null) {
            histBtnLast.setDisable(histCurrentPage >= totalPages - 1);
        }

        if (histPageBox != null) {
            histPageBox.getChildren().clear();
            int maxBtn = 5;
            int startP = Math.max(0, histCurrentPage - 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn) {
                startP = Math.max(0, endP - maxBtn);
            }
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                Button btn = new Button(String.valueOf(p + 1));
                btn.getStyleClass().add(p == histCurrentPage
                        ? "pay-pagination-active"
                        : "pay-pagination-inactive");
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
}