package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.AlertUtils;
import com.auction.model.dto.*;
import com.auction.model.entity.BidTransaction;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.auction.client.observer.AuctionEventObserver;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý chi tiết phiên đấu giá trực tuyến.
 */
public class AuctionDetailController implements AuctionEventObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDetailController.class);

    // THÔNG TIN CHI TIẾT SẢN PHẨM FXML (KHUNG BÊN TRÁI)
    @FXML private Label lblBreadcrumbName, lblDetailName, lblDetailStatus, lblDetailEmoji, lblDetailDesc;
    @FXML private javafx.scene.image.ImageView imgProduct;
    @FXML private GridPane gridSpecs;

    // THÔNG TIN BƯỚC GIÁ & ĐẾM NGƯỢC FXML (KHUNG BÊN PHẢI)
    @FXML private Label lblCurrentPrice, lblStartPrice, lblBidStep;
    @FXML private Label lblCdH, lblCdM, lblCdS;

    // KHUNG ĐIỀU KHIỂN & ĐẶT GIÁ FXML
    @FXML private HBox paneAntiSnipe;
    @FXML private VBox paneWinner, paneBidForm, paneAutoBidStatus, paneAutoBidForm;
    @FXML private Label lblWinnerName, lblAutoBidDetail;
    @FXML private TextField txtBidAmount, txtAutoMax, txtAutoInc;

    // BẢNG LỊCH SỬ ĐẶT GIÁ FXML
    @FXML private TableView<BidTransaction> tableHistory;
    @FXML private TableColumn<BidTransaction, String> colUser;
    @FXML private TableColumn<BidTransaction, Double> colAmount;
    @FXML private TableColumn<BidTransaction, LocalDateTime> colTime;

    // BIỂU ĐỒ BƯỚC GIÁ LINE CHART FXML
    @FXML private LineChart<String, Number> bidChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    private String auctionId;
    private AuctionDetailDTO currentAuction;
    private Timeline countdownTimeline;
    private final ObservableList<BidTransaction> bidHistory = FXCollections.observableArrayList();

    public void initData(String auctionId) {
        this.auctionId = auctionId;
        setupTable();
        setupChart();
        loadAuctionDetail();
        subscribeToUpdates();
    }

    /**
     * Cấu hình TableView lịch sử đặt giá.
     */
    private void setupTable() {
        colUser.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
        colUser.setCellFactory(_ -> new TableCell<>() {
            private final VBox box = new VBox(2);
            private final Label nameLbl = new Label();
            private final Label autoLbl = new Label("⚡ AUTO");

            {
                autoLbl.getStyleClass().add("auc-detail-auto-label");
                box.getChildren().addAll(nameLbl, autoLbl);
            }

            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    BidTransaction row = getTableView().getItems().get(getIndex());
                    if (row.isAutoBid()) {
                        nameLbl.setText(username);
                        setGraphic(box);
                        setText(null);
                    } else {
                        setText(username);
                        setGraphic(null);
                    }
                }
            }
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colAmount.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    getStyleClass().remove("auc-detail-first-row-price");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    if (getIndex() == 0) {
                        if (!getStyleClass().contains("auc-detail-first-row-price")) {
                            getStyleClass().add("auc-detail-first-row-price");
                        }
                    } else {
                        getStyleClass().remove("auc-detail-first-row-price");
                    }
                }
            }
        });

        colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colTime.setCellFactory(_ -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss\ndd/MM/yyyy");

            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null)
                    setText(null);
                else
                    setText(time.format(fmt));
            }
        });

        tableHistory.setItems(bidHistory);
        tableHistory.setPlaceholder(new Label("Chưa có lượt đấu giá nào"));
    }

    /**
     * Cấu hình LineChart biểu diễn tiến độ bước giá.
     */
    private void setupChart() {
        bidChart.setCreateSymbols(true);
        bidChart.setAnimated(false);
        bidChart.setLegendVisible(false);
        bidChart.getStyleClass().add("auc-detail-chart-bg");
        chartXAxis.setLabel("");
        chartXAxis.setTickLabelRotation(0);
        chartYAxis.setLabel("");
        chartYAxis.setForceZeroInRange(false);
    }

    /**
     * Tải thông tin chi tiết phiên đấu giá từ server.
     */
    private void loadAuctionDetail() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_AUCTION_DETAIL, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    currentAuction = res.getPayloadAs(AuctionDetailDTO.class);
                    Platform.runLater(this::updateUI);
                    loadBidHistory();
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối",
                            "Không thể nạp chi tiết phiên đấu giá từ server."));
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố khi nạp thông tin chi tiết phiên đấu giá từ máy chủ", e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi hệ thống",
                        "Lỗi kết nối máy chủ: " + e.getMessage()));
            }
        });
    }

    /**
     * Tải lịch sử đấu giá từ server.
     */
    private void loadBidHistory() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_BID_HISTORY, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    List<BidTransaction> history = res.getPayloadAs(new TypeToken<>() {
                    });
                    Platform.runLater(() -> {
                        bidHistory.setAll(history);
                        updateChart(history);
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố khi tải lịch sử đặt giá từ máy chủ", e);
            }
        });
    }

    /**
     * Cập nhật các trường hiển thị UI.
     */
    private void updateUI() {
        if (currentAuction == null)
            return;

        lblDetailName.setText(currentAuction.getItemName());
        lblBreadcrumbName.setText(currentAuction.getItemName());
        lblDetailDesc
                .setText(currentAuction.getDescription() != null ? currentAuction.getDescription() : "Không có mô tả.");

        if (currentAuction.getImageBase64() != null && !currentAuction.getImageBase64().trim().isEmpty()) {
            try {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(currentAuction.getImageBase64().trim());
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes);
                javafx.scene.image.Image image = new javafx.scene.image.Image(bis);
                imgProduct.setImage(image);
                imgProduct.setVisible(true);
                lblDetailEmoji.setVisible(false);
            } catch (Exception e) {
                LOGGER.error("Lỗi hiển thị ảnh Base64 trong chi tiết", e);
                imgProduct.setVisible(false);
                lblDetailEmoji.setVisible(true);
            }
        } else {
            imgProduct.setVisible(false);
            lblDetailEmoji.setVisible(true);
        }

        lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentAuction.getCurrentPrice()));
        lblStartPrice.setText(String.format("Giá khởi điểm: %,.0f VNĐ", currentAuction.getStartingPrice()));
        lblBidStep.setText(String.format("Bước giá tối thiểu: %,.0f VNĐ", currentAuction.getStepPrice()));

        String status = currentAuction.getStatus();
        updateStatusBadge(status);
        updateEmoji(currentAuction.getItemDetails());

        boolean isRunning = "RUNNING".equalsIgnoreCase(status);
        boolean isFinished = "FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);

        paneBidForm.setVisible(isRunning);
        paneBidForm.setManaged(isRunning);

        paneWinner.setVisible(isFinished && currentAuction.getCurrentWinnerName() != null);
        paneWinner.setManaged(isFinished && currentAuction.getCurrentWinnerName() != null);
        if (isFinished && currentAuction.getCurrentWinnerName() != null) {
            lblWinnerName.setText(currentAuction.getCurrentWinnerName());
        }

        if (isRunning) {
            startCountdown();
        } else {
            stopCountdown();
        }

        buildSpecsGrid();
    }

    private void updateStatusBadge(String status) {
        lblDetailStatus.getStyleClass().removeAll("status-running", "status-open", "status-finished", "status-paid", "status-canceled");
        switch (status.toUpperCase()) {
            case "RUNNING":
                lblDetailStatus.setText("● ĐANG DIỄN RA");
                lblDetailStatus.getStyleClass().add("status-running");
                break;
            case "OPEN":
                lblDetailStatus.setText("● SẮP DIỄN RA");
                lblDetailStatus.getStyleClass().add("status-open");
                break;
            case "FINISHED":
                lblDetailStatus.setText("● ĐÃ KẾT THÚC");
                lblDetailStatus.getStyleClass().add("status-finished");
                break;
            case "PAID":
                lblDetailStatus.setText("● ĐÃ THANH TOÁN");
                lblDetailStatus.getStyleClass().add("status-paid");
                break;
            case "CANCELED":
            case "CANCELLED":
                lblDetailStatus.setText("● ĐÃ HỦY");
                lblDetailStatus.getStyleClass().add("status-canceled");
                break;
            default:
                lblDetailStatus.setText(status);
        }
    }

    private void updateEmoji(String itemDetails) {
        if (itemDetails == null) {
            lblDetailEmoji.setText("📦");
            return;
        }
        String details = itemDetails.toLowerCase();
        if (details.contains("laptop") || details.contains("macbook") || details.contains("máy tính")) {
            lblDetailEmoji.setText("💻");
        } else if (details.contains("điện thoại") || details.contains("iphone") || details.contains("samsung")) {
            lblDetailEmoji.setText("📱");
        } else if (details.contains("xe") || details.contains("ô tô") || details.contains("xe máy")
                || details.contains("honda") || details.contains("vinfast") || details.contains("toyota")) {
            lblDetailEmoji.setText("🚗");
        } else if (details.contains("tranh") || details.contains("nghệ thuật") || details.contains("gốm")
                || details.contains("sơn mài")) {
            lblDetailEmoji.setText("🎨");
        } else if (details.contains("tủ lạnh") || details.contains("điện tử")) {
            lblDetailEmoji.setText("🖥️");
        } else {
            lblDetailEmoji.setText("📦");
        }
    }

    private void buildSpecsGrid() {
        gridSpecs.getChildren().clear();
        if (currentAuction.getItemDetails() == null)
            return;

        String[] lines = currentAuction.getItemDetails().split("\n");
        int row = 0;
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                Label key = new Label(parts[0].trim());
                key.getStyleClass().add("auc-detail-spec-key");
                Label val = new Label(parts[1].trim());
                val.getStyleClass().add("auc-detail-spec-val");
                gridSpecs.add(key, 0, row);
                gridSpecs.add(val, 1, row);
                row++;
            }
        }
    }

    private void updateChart(List<BidTransaction> history) {
        bidChart.getData().clear();
        if (history == null || history.isEmpty())
            return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá đấu");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (BidTransaction bid : history) {
            String timeLabel = bid.getBidTime() != null ? bid.getBidTime().format(fmt) : "?";
            series.getData().add(new XYChart.Data<>(timeLabel, bid.getBidAmount()));
        }

        bidChart.getData().add(series);

        if (series.getNode() != null) {
            series.getNode().getStyleClass().add("bid-chart-series");
        }
    }

    /**
     * Kích hoạt đếm ngược thời gian phiên.
     */
    private void startCountdown() {
        stopCountdown();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            if (currentAuction == null || currentAuction.getEndTime() == null)
                return;
            long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), currentAuction.getEndTime());
            if (seconds <= 0) {
                lblCdH.setText("00");
                lblCdM.setText("00");
                lblCdS.setText("00");
                stopCountdown();
                loadAuctionDetail();
                return;
            }
            lblCdH.setText(String.format("%02d", seconds / 3600));
            lblCdM.setText(String.format("%02d", (seconds % 3600) / 60));
            lblCdS.setText(String.format("%02d", seconds % 60));

            if (seconds < 60) {
                paneAntiSnipe.setVisible(true);
                paneAntiSnipe.setManaged(true);
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void subscribeToUpdates() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request subReq = new Request(RequestType.SUBSCRIBE_AUCTION, auctionId);
                ClientSocketManager.getInstance().sendRequest(subReq);
                ClientSocketManager.getInstance().addObserver(this);
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố ngắt kết nối mạng khi gửi yêu cầu SUBSCRIBE_AUCTION", e);
            }
        });
    }

    @Override
    public void onAuctionEvent(String event, String eventAuctionId, JsonObject payload) {
        // Kiểm tra xem sự kiện nhận được có đúng với phiên đấu giá hiện tại đang xem hay không
        if (!auctionId.equals(eventAuctionId))
            return;

        switch (event) {
            case "BID_UPDATE" -> {
                // Nhận giá trị đặt thầu mới nhất từ payload JSON
                double newPrice = payload.has("newPrice") ? payload.get("newPrice").getAsDouble() : 0;
                // Cập nhật lên nhãn hiển thị giá hiện tại trên UI
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                if (currentAuction != null) {
                    currentAuction.setCurrentPrice(newPrice);
                    // Cập nhật đồng hồ đếm ngược nếu bị gia hạn (Anti-sniping)
                    if (payload.has("newEndTime") && !payload.get("newEndTime").isJsonNull()) {
                        currentAuction.setEndTime(LocalDateTime.parse(payload.get("newEndTime").getAsString()));
                        startCountdown(); // Restart countdown
                        paneAntiSnipe.setVisible(true);
                        paneAntiSnipe.setManaged(true);
                    }
                }
                // Nạp lại danh sách lịch sử đặt giá để hiển thị dòng mới nhất lên bảng
                loadBidHistory();
            }
            case "ANTI_SNIPE" -> {
                // Cơ chế chống bắn tỉa (Anti-Sniping): Nếu có người đặt giá ở những giây cuối, thời gian kết thúc được tự động gia hạn thêm
                if (payload.has("newEndTime") && currentAuction != null) {
                    currentAuction.setEndTime(LocalDateTime.parse(payload.get("newEndTime").getAsString()));
                }
                // Hiển thị nhãn cảnh báo kích hoạt Anti-Snipe trên UI để thông báo cho người dùng
                paneAntiSnipe.setVisible(true);
                paneAntiSnipe.setManaged(true);
            }
            case "AUCTION_CLOSED" -> {
                // Sự kiện đóng phiên: Dừng bộ đếm ngược thời gian và tải lại chi tiết phiên để cập nhật giao diện kết quả chung cuộc
                stopCountdown();
                loadAuctionDetail();
            }
        }
    }

    /**
     * Thực hiện đặt giá thủ công.
     */
    @FXML
    void handlePlaceBid() {
        String amountStr = txtBidAmount.getText().trim();
        // Kiểm tra xem ô nhập số tiền đặt thầu có bị bỏ trống hay không
        if (amountStr.isEmpty()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập số tiền muốn đặt.");
            return;
        }
        try {
            // Lọc bỏ tất cả ký tự định dạng (chấm, phẩy) để chuyển thành số double thuần túy
            double amount = Double.parseDouble(amountStr.replace(",", "").replace(".", ""));

            double minRequiredBid;
            boolean isFirstBid = bidHistory.isEmpty();
            // Trường hợp 1: Chưa từng có ai tham gia đặt giá cho sản phẩm này
            if (isFirstBid) {
                minRequiredBid = currentAuction.getStartingPrice();
                if (amount < minRequiredBid) {
                    AlertUtils.showWarning("Lỗi đặt giá",
                            "Giá đặt phải lớn hơn hoặc bằng giá khởi điểm: "
                                    + String.format("%,.0f VNĐ", minRequiredBid));
                    return;
                }
            } 
            // Trường hợp 2: Đã có người đặt giá trước đó, yêu cầu giá mới phải lớn hơn hoặc bằng (Giá hiện hành + Bước giá)
            else {
                minRequiredBid = currentAuction.getCurrentPrice() + currentAuction.getStepPrice();
                if (amount < minRequiredBid) {
                    AlertUtils.showWarning("Lỗi đặt giá",
                            "Giá đặt tối thiểu phải là " + String.format("%,.0f VNĐ", minRequiredBid)
                                    + " (Giá hiện tại + Bước giá tối thiểu "
                                    + String.format("%,.0f VNĐ", currentAuction.getStepPrice()) + ")");
                    return;
                }
            }

            // Đóng gói dữ liệu yêu cầu đặt thầu và gửi bất đồng bộ lên Server
            BidRequestDTO dto = new BidRequestDTO(auctionId, amount);
            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Response res = ClientSocketManager.getInstance()
                            .sendRequest(new Request(RequestType.PLACE_BID, dto));
                    Platform.runLater(() -> {
                        if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                            txtBidAmount.clear(); // Dọn sạch ô nhập liệu trên UI sau khi giao dịch thành công
                        } else {
                            AlertUtils.showWarning("Đặt giá thất bại",
                                    res != null ? res.getMessage() : "Lỗi kết nối");
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố hệ thống khi thực hiện hành động đặt giá", e);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi", e.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Lỗi", "Số tiền không hợp lệ. Chỉ nhập số.");
        }
    }

    /**
     * Đăng ký đấu giá tự động.
     */
    @FXML
    void handleRegisterAutoBid() {
        String maxStr = txtAutoMax.getText().trim();
        String incStr = txtAutoInc.getText().trim();
        if (maxStr.isEmpty() || incStr.isEmpty()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập đủ giá tối đa và bước tăng.");
            return;
        }
        try {
            double maxBid = Double.parseDouble(maxStr.replace(",", "").replace(".", ""));
            double incBid = Double.parseDouble(incStr.replace(",", "").replace(".", ""));

            AutoBidDTO dto = new AutoBidDTO(auctionId, maxBid, incBid);
            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Response res = ClientSocketManager.getInstance()
                            .sendRequest(new Request(RequestType.REGISTER_AUTO_BID, dto));
                    Platform.runLater(() -> {
                        if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                            paneAutoBidForm.setVisible(false);
                            paneAutoBidForm.setManaged(false);
                            paneAutoBidStatus.setVisible(true);
                            paneAutoBidStatus.setManaged(true);
                            lblAutoBidDetail.setText(
                                    String.format("Max: %,.0fđ | Inc: %,.0fđ", dto.getMaxBid(), dto.getIncrement()));
                        } else {
                            AlertUtils.showWarning("Lỗi Tự động đặt giá",
                                    res != null ? res.getMessage() : "Lỗi kết nối");
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố lỗi nghiệp vụ khi đăng ký tự động đặt giá", e);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi",
                            String.format("Đã xảy ra lỗi: %s", e.getMessage())));
                }
            });
        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Lỗi",
                    "Số tiền không hợp lệ. Vui lòng chỉ nhập số (có thể dùng dấu phẩy hoặc chấm).");
        }
    }

    /**
     * Hủy bỏ cơ chế đấu giá tự động.
     */
    @FXML
    void handleCancelAutoBid() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Response res = ClientSocketManager.getInstance()
                        .sendRequest(new Request(RequestType.CANCEL_AUTO_BID, auctionId));
                Platform.runLater(() -> {
                    if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                        paneAutoBidStatus.setVisible(false);
                        paneAutoBidStatus.setManaged(false);
                        paneAutoBidForm.setVisible(true);
                        paneAutoBidForm.setManaged(true);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp lỗi mạng khi gửi chỉ thị hủy bỏ cơ chế Auto-Bid của phiên: {}", auctionId, e);
            }
        });
    }

    @FXML
    public void goBack() {
        stopCountdown();
        ClientSocketManager.getInstance().removeObserver(this);
        if (MainController.getInstance() != null) {
            MainController.getInstance().showPageList();
        }
    }
}