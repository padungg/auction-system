package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bộ điều khiển phân hệ Giao dịch và Thanh toán (Payment Controller).
 * Đảm nhiệm vai trò quản lý các khoản thanh toán đang chờ xử lý, hiển thị danh sách hóa đơn trúng đấu giá,
 * kết xuất giao diện thẻ (Card UI) động, và quản lý lịch sử tất toán tiền tệ đi kèm cơ chế phân trang.
 */
public class PaymentController {

    /**
     * Khởi tạo hệ thống ghi nhật ký log tập trung theo tiêu chuẩn SLF4J.
     * Hỗ trợ lưu vết dòng chảy dữ liệu, ghi nhận các lỗi ngắt kết nối I/O Socket trong luồng giao dịch tài chính
     * và phân tích hành vi nạp/rút tiền của tài khoản người dùng Client.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    @FXML private Button btnTabPending;
    @FXML private Button btnTabHistory;

    @FXML private VBox panelPending;
    @FXML private VBox panelHistory;
    @FXML private VBox cardContainer;
    @FXML private Label lblNoPending;

    @FXML private TableView<AuctionSummaryDTO> tableHistory;
    @FXML private TableColumn<AuctionSummaryDTO, String> colHistName;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colHistPrice;
    @FXML private TableColumn<AuctionSummaryDTO, String> colHistTime;
    @FXML private TableColumn<AuctionSummaryDTO, String> colHistStatus;

    // Pagination
    @FXML private HBox histPageBox;
    @FXML private Label histPageInfo;
    @FXML private Button histBtnFirst;
    @FXML private Button histBtnPrev;
    @FXML private Button histBtnNext;
    @FXML private Button histBtnLast;

    private static final int HIST_PAGE_SIZE = 10;
    private int histCurrentPage = 0;
    private java.util.List<AuctionSummaryDTO> allHistory = new java.util.ArrayList<>();
    private ObservableList<AuctionSummaryDTO> historyList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupHistoryTable();
        loadPending();
        loadHistory();
    }

    private void setupHistoryTable() {
        colHistName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colHistPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else { setText(String.format("%,.0f VNĐ", price)); setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981;"); }
            }
        });
        colHistTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colHistStatus.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                Label lbl = new Label("✅ Đã thanh toán");
                lbl.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 3 10; -fx-background-radius: 99; -fx-font-weight: bold; -fx-font-size: 11px;");
                setGraphic(lbl);
                setAlignment(Pos.CENTER);
            }
        });
        tableHistory.setItems(historyList);
        tableHistory.setFixedCellSize(50);
    }

    @FXML void switchTabPending(ActionEvent e) {
        setTabActive(btnTabPending);
        setTabInactive(btnTabHistory);
        panelPending.setVisible(true); panelPending.setManaged(true);
        panelHistory.setVisible(false); panelHistory.setManaged(false);
        loadPending();
    }

    @FXML void switchTabHistory(ActionEvent e) {
        setTabActive(btnTabHistory);
        setTabInactive(btnTabPending);
        panelHistory.setVisible(true); panelHistory.setManaged(true);
        panelPending.setVisible(false); panelPending.setManaged(false);
        loadHistory();
    }

    private void setTabActive(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 13.5; -fx-padding: 10 22; -fx-border-color: transparent transparent #2563eb transparent; -fx-border-width: 0 0 2 0; -fx-cursor: hand;");
    }
    private void setTabInactive(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13.5; -fx-padding: 10 22; -fx-cursor: hand;");
    }

    private void loadPending() {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.GET_PENDING_PAYMENTS, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res.getStatus() == ResponseStatus.SUCCESS) {
                    Gson gson = ClientSocketManager.getInstance().getGson();
                    AuctionSummaryDTO[] arr = gson.fromJson(gson.toJson(res.getPayload()), AuctionSummaryDTO[].class);
                    Platform.runLater(() -> buildPendingCards(arr));
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi tải chờ thanh toán", e);
            }
        }).start();
    }

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

    private HBox buildCard(AuctionSummaryDTO dto, double userBalance) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 18 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 6, 0, 0, 2);");

        // Icon / avatar
        Label icon = new Label("🖥");
        icon.setStyle("-fx-font-size: 38; -fx-min-width: 60; -fx-alignment: center;");
        if ("ART".equalsIgnoreCase(dto.getItemType())) icon.setText("🎨");
        else if ("VEHICLE".equalsIgnoreCase(dto.getItemType())) icon.setText("🚗");

        // Info block
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(dto.getItemName() != null ? dto.getItemName() : "Không rõ");
        nameLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        // Type badge
        Label typeBadge = new Label(dto.getItemType() != null ? dto.getItemType() : "");
        String badgeColor = "#f3e8ff", badgeText = "#7e22ce";
        if ("ART".equalsIgnoreCase(dto.getItemType())) { badgeColor = "#fce7f3"; badgeText = "#be185d"; }
        else if ("VEHICLE".equalsIgnoreCase(dto.getItemType())) { badgeColor = "#d1fae5"; badgeText = "#047857"; }
        typeBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: " + badgeText + "; -fx-padding: 2 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 10px;");

        HBox metaBox = new HBox(8);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.getChildren().add(typeBadge);

        Label condLabel = new Label("🟡 Tình trạng: Như mới");
        condLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");
        metaBox.getChildren().add(condLabel);

        Label endLabel = new Label("⏱ Kết thúc: " + (dto.getEndTime() != null ? dto.getEndTime() : "N/A"));
        endLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");

        // Deadline warning
        String deadline = "3 ngày sau khi kết thúc";
        HBox deadlineBox = new HBox(6);
        deadlineBox.setAlignment(Pos.CENTER_LEFT);
        deadlineBox.setStyle("-fx-background-color: #fef9c3; -fx-padding: 4 10; -fx-background-radius: 6;");
        Label deadlineIcon = new Label("⏰");
        Label deadlineLabel = new Label("Hạn thanh toán: " + deadline);
        deadlineLabel.setStyle("-fx-text-fill: #854d0e; -fx-font-size: 11; -fx-font-weight: bold;");
        deadlineBox.getChildren().addAll(deadlineIcon, deadlineLabel);

        Label auctionIdLabel = new Label("Mã phiên: #" + dto.getAuctionId());
        auctionIdLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");

        info.getChildren().addAll(nameLabel, metaBox, endLabel, deadlineBox, auctionIdLabel);

        // Price + actions block
        double price = dto.getCurrentPrice();
        double fee = price * 0.02;
        double total = price + fee;
        boolean canPay = userBalance >= total;

        VBox priceBlock = new VBox(4);
        priceBlock.setAlignment(Pos.TOP_RIGHT);
        priceBlock.setMinWidth(200);

        Label priceCaption = new Label("GIÁ TRÚNG");
        priceCaption.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10; -fx-font-weight: bold;");

        Label priceLabel = new Label(String.format("%,.0f VNĐ", price));
        priceLabel.setStyle("-fx-font-size: 18; -fx-font-weight: 800; -fx-text-fill: #2563eb;");

        Label feeLabel = new Label(String.format("Phí DV (2%%): %,.0f VNĐ", fee));
        feeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");

        Label totalLabel = new Label(String.format("Tổng: %,.0f VNĐ", total));
        totalLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 13;");

        Button btnView = new Button("🔍 Xem sản phẩm");
        btnView.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-size: 11; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 10;");
        btnView.setOnAction(e -> MainController.getInstance().openAuctionDetail(dto.getAuctionId()));

        Button btnPay = new Button("💳 Nạp tiền & Thanh toán");
        if (canPay) {
            btnPay.setText("💳 Thanh toán ngay");
            btnPay.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;");
        } else {
            btnPay.setStyle("-fx-background-color: #f87171; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;");
        }
        btnPay.setOnAction(e -> handlePayForAuction(dto));

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(btnView, btnPay);

        if (!canPay) {
            double needed = total - userBalance;
            Label balWarning = new Label(String.format("⚠ Số dư không đủ — cần thêm %,.0f VNĐ", needed));
            balWarning.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11; -fx-font-style: italic;");
            priceBlock.getChildren().addAll(priceCaption, priceLabel, feeLabel, totalLabel, btnRow, balWarning);
        } else {
            priceBlock.getChildren().addAll(priceCaption, priceLabel, feeLabel, totalLabel, btnRow);
        }

        card.getChildren().addAll(icon, info, priceBlock);
        return card;
    }

    private void handlePayForAuction(AuctionSummaryDTO dto) {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.PAY_AUCTION, dto.getAuctionId());
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                Platform.runLater(() -> {
                    if (res.getStatus() == ResponseStatus.SUCCESS) {
                        showSuccess("Thanh toán thành công! 🎉",
                                "Bạn đã thanh toán thành công cho sản phẩm: " + dto.getItemName());
                        loadPending();
                        loadHistory();
                        if (MainController.getInstance() != null) {
                            MainController.getInstance().refreshBalanceFromServer();
                        }
                    } else {
                        showAlert("Lỗi thanh toán", res.getMessage() != null ? res.getMessage() : "Không thể thanh toán.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    private void loadHistory() {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.GET_PAYMENT_HISTORY, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res.getStatus() == ResponseStatus.SUCCESS) {
                    Gson gson = ClientSocketManager.getInstance().getGson();
                    AuctionSummaryDTO[] arr = gson.fromJson(gson.toJson(res.getPayload()), AuctionSummaryDTO[].class);
                    Platform.runLater(() -> {
                        allHistory.clear();
                        if (arr != null) java.util.Collections.addAll(allHistory, arr);
                        histCurrentPage = 0;
                        renderHistPage();
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi tải lịch sử thanh toán", e);
            }
        }).start();
    }

    // Pagination

    private void renderHistPage() {
        int total = allHistory.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / HIST_PAGE_SIZE));
        int from = histCurrentPage * HIST_PAGE_SIZE;
        int to   = Math.min(from + HIST_PAGE_SIZE, total);

        historyList.clear();
        historyList.addAll(allHistory.subList(from, to));

        if (histPageInfo != null)
            histPageInfo.setText("Trang " + (histCurrentPage + 1) + " / " + totalPages
                    + "  (" + total + " giao dịch)");
        if (histBtnFirst != null) histBtnFirst.setDisable(histCurrentPage == 0);
        if (histBtnPrev  != null) histBtnPrev.setDisable(histCurrentPage == 0);
        if (histBtnNext  != null) histBtnNext.setDisable(histCurrentPage >= totalPages - 1);
        if (histBtnLast  != null) histBtnLast.setDisable(histCurrentPage >= totalPages - 1);

        if (histPageBox != null) {
            histPageBox.getChildren().clear();
            int maxBtn = 5, startP = Math.max(0, histCurrentPage - 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn) startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                Button btn = new Button(String.valueOf(p + 1));
                btn.setStyle(p == histCurrentPage
                        ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 30;"
                        : "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 30;");
                btn.setOnAction(e -> { histCurrentPage = pg; renderHistPage(); });
                histPageBox.getChildren().add(btn);
            }
        }
    }

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

    private void showSuccess(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}