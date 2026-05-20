package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.*;
import com.auction.model.entity.BidTransaction;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
 * Bộ điều khiển phân hệ Chi tiết phiên đấu giá (Auction Detail Controller).
 * Đảm nhiệm vai trò quản lý thông tin sản phẩm, hiển thị biểu đồ biến động giá, đếm ngược thời gian thực,
 * tiếp nhận tương tác đặt giá thủ công / tự động (Auto-Bid) và đồng bộ trạng thái qua mẫu thiết kế Observer Pattern.
 */
public class AuctionDetailController implements AuctionEventObserver {

    /**
     * Khởi tạo thành phần Logger theo tiêu chuẩn SLF4J nhằm phục vụ công tác giám sát luồng vận hành,
     * lưu vết các lỗi kết nối Socket bất đồng bộ và theo dõi lịch sử đặt giá thời gian thực.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDetailController.class);

    private final Gson GSON = GsonProvider.getInstance();

    // Nhóm nhãn thông tin cột bên trái
    @FXML private Label lblBreadcrumbName, lblDetailName, lblDetailStatus, lblDetailEmoji, lblDetailDesc;
    @FXML private GridPane gridSpecs;

    // Nhóm thông tin giá và đếm ngược cột bên phải
    @FXML private Label lblCurrentPrice, lblStartPrice, lblBidStep;
    @FXML private Label lblCdH, lblCdM, lblCdS;

    // Nhóm giao diện chức năng cột bên phải
    @FXML private HBox paneAntiSnipe;
    @FXML private VBox paneWinner, paneBidForm, paneAutoBidStatus, paneAutoBidForm;
    @FXML private Label lblWinnerName, lblAutoBidDetail;

    // Biểu mẫu nhập liệu đặt giá
    @FXML private TextField txtBidAmount, txtAutoMax, txtAutoInc;

    // Bảng lịch sử đặt giá
    @FXML private TableView<BidTransaction> tableHistory;
    @FXML private TableColumn<BidTransaction, String> colUser;
    @FXML private TableColumn<BidTransaction, Double> colAmount;
    @FXML private TableColumn<BidTransaction, LocalDateTime> colTime;

    // Biểu đồ biến động giá đấu
    @FXML private LineChart<String, Number> bidChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    private String auctionId;
    private AuctionDetailDTO currentAuction;
    private Timeline countdownTimeline;
    private final ObservableList<BidTransaction> bidHistory = FXCollections.observableArrayList();

    // INIT

    /**
     * Tiếp nhận mã định danh phiên đấu giá, cấu hình cấu trúc bảng, biểu đồ và kích hoạt luồng tải dữ liệu mạng.
     */
    public void initData(String auctionId) {
        this.auctionId = auctionId;
        setupTable();
        setupChart();
        loadAuctionDetail();
        subscribeToUpdates();
    }

    /**
     * Định hình quy tắc liên kết thuộc tính DTO và tùy biến đồ họa phân cấp dòng (Highlight hàng đầu tiên, gắn nhãn AUTO).
     */
    private void setupTable() {
        colUser.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
        colUser.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    BidTransaction row = getTableView().getItems().get(getIndex());
                    if (row.isAutoBid()) {
                        VBox box = new VBox(2);
                        Label nameLbl = new Label(username);
                        Label autoLbl = new Label("⚡ AUTO");
                        autoLbl.setStyle("-fx-text-fill: #d97706; -fx-font-size: 10px; -fx-font-weight: bold;");
                        box.getChildren().addAll(nameLbl, autoLbl);
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
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    // Làm nổi bật mức giá cao nhất hiện tại (dòng đầu tiên)
                    if (getIndex() == 0) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colTime.setCellFactory(tc -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss\ndd/MM/yyyy");
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) setText(null);
                else setText(time.format(fmt));
            }
        });

        tableHistory.setItems(bidHistory);
        tableHistory.setPlaceholder(new Label("Chưa có lượt đấu giá nào"));
    }

    /**
     * Thiết lập cấu hình đồ họa nền tảng cho biểu đồ tuyến tính LineChart.
     */
    private void setupChart() {
        bidChart.setCreateSymbols(true);
        bidChart.setAnimated(false);
        bidChart.setLegendVisible(false);
        bidChart.setStyle("-fx-background-color: transparent;");
        chartXAxis.setLabel("");
        chartYAxis.setLabel("");
        chartYAxis.setForceZeroInRange(false);
    }

    // DATA LOADING

    /**
     * Khởi chạy Worker Thread đồng bộ thông tin chi tiết của phiên đấu giá chỉ định từ Server.
     */
    private void loadAuctionDetail() {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.GET_AUCTION_DETAIL, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    currentAuction = GSON.fromJson(GSON.toJson(res.getPayload()), AuctionDetailDTO.class);
                    Platform.runLater(this::updateUI);
                    loadBidHistory();
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố khi nạp thông tin chi tiết phiên đấu giá từ máy chủ", e);
            }
        }).start();
    }

    /**
     * Tải danh sách lịch sử các lượt đặt giá đã diễn ra của phiên đấu giá hiện hành.
     */
    private void loadBidHistory() {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.GET_BID_HISTORY, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    List<BidTransaction> history = GSON.fromJson(
                            GSON.toJson(res.getPayload()),
                            new com.google.gson.reflect.TypeToken<List<BidTransaction>>() {}.getType());
                    Platform.runLater(() -> {
                        bidHistory.setAll(history);
                        updateChart(history);
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố khi tải lịch sử đặt giá từ máy chủ", e);
            }
        }).start();
    }

    // UI UPDATE

    /**
     * Ánh xạ thông tin cấu trúc dữ liệu của thực thể phiên đấu giá lên các thành phần giao diện JavaFX.
     */
    private void updateUI() {
        if (currentAuction == null) return;

        // Thanh điều hướng và tiêu đề chính
        lblDetailName.setText(currentAuction.getItemName());
        lblBreadcrumbName.setText(currentAuction.getItemName());
        lblDetailDesc.setText(currentAuction.getDescription() != null ? currentAuction.getDescription() : "Không có mô tả.");

        // Thông tin giá tệ
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentAuction.getCurrentPrice()));
        lblStartPrice.setText(String.format("Giá khởi điểm: %,.0f VNĐ", currentAuction.getStartingPrice()));

        // Trạng thái Badge màu sắc
        String status = currentAuction.getStatus();
        updateStatusBadge(status);

        // Biểu tượng cảm xúc (Emoji) căn cứ theo từ khóa phân loại danh mục
        updateEmoji(currentAuction.getItemDetails());

        // Kiểm soát ẩn hiện các phân hệ chức năng tương ứng với vòng đời phiên
        boolean isRunning = "RUNNING".equalsIgnoreCase(status);
        boolean isFinished = "FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);

        paneBidForm.setVisible(isRunning);
        paneBidForm.setManaged(isRunning);

        paneWinner.setVisible(isFinished && currentAuction.getCurrentWinnerName() != null);
        paneWinner.setManaged(isFinished && currentAuction.getCurrentWinnerName() != null);
        if (isFinished && currentAuction.getCurrentWinnerName() != null) {
            lblWinnerName.setText(currentAuction.getCurrentWinnerName());
        }

        // Kích hoạt hoặc hủy bỏ bộ đếm ngược
        if (isRunning) {
            startCountdown();
        } else {
            stopCountdown();
        }

        // Xây dựng bảng lưới thông số thông tin sản phẩm
        buildSpecsGrid();
    }

    /**
     * Cập nhật văn bản và gắn thẻ phong cách CSS Style riêng biệt cho từng trạng thái của phiên đấu giá.
     */
    private void updateStatusBadge(String status) {
        lblDetailStatus.getStyleClass().removeAll("status-running", "status-open", "status-finished", "status-paid");
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
            default:
                lblDetailStatus.setText(status);
        }
    }

    /**
     * Phân tích chuỗi chi tiết sản phẩm để gán biểu tượng Emoji minh họa trực quan phù hợp.
     */
    private void updateEmoji(String itemDetails) {
        if (itemDetails == null) { lblDetailEmoji.setText("📦"); return; }
        String details = itemDetails.toLowerCase();
        if (details.contains("laptop") || details.contains("macbook") || details.contains("máy tính")) {
            lblDetailEmoji.setText("💻");
        } else if (details.contains("điện thoại") || details.contains("iphone") || details.contains("samsung")) {
            lblDetailEmoji.setText("📱");
        } else if (details.contains("xe") || details.contains("ô tô") || details.contains("xe máy") || details.contains("honda") || details.contains("vinfast") || details.contains("toyota")) {
            lblDetailEmoji.setText("🚗");
        } else if (details.contains("tranh") || details.contains("nghệ thuật") || details.contains("gốm") || details.contains("sơn mài")) {
            lblDetailEmoji.setText("🎨");
        } else if (details.contains("tủ lạnh") || details.contains("điện tử")) {
            lblDetailEmoji.setText("🖥️");
        } else {
            lblDetailEmoji.setText("📦");
        }
    }

    /**
     * Bóc tách dữ liệu có cấu trúc định dạng dấu hai chấm (Key: Value) để phân bổ vào bảng lưới thông số.
     */
    private void buildSpecsGrid() {
        gridSpecs.getChildren().clear();
        if (currentAuction.getItemDetails() == null) return;

        String[] lines = currentAuction.getItemDetails().split("\n");
        int row = 0;
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                Label key = new Label(parts[0].trim());
                key.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
                Label val = new Label(parts[1].trim());
                val.setStyle("-fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 13;");
                gridSpecs.add(key, 0, row);
                gridSpecs.add(val, 1, row);
                row++;
            }
        }
    }

    /**
     * Dựng luồng biểu đồ thời gian thực hiển thị tiến trình biến động giá đấu từ cũ nhất đến mới nhất.
     */
    private void updateChart(List<BidTransaction> history) {
        bidChart.getData().clear();
        if (history == null || history.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá đấu");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (int i = 0; i < history.size(); i++) {
            BidTransaction bid = history.get(i);
            String timeLabel = bid.getBidTime() != null ? bid.getBidTime().format(fmt) : "?";
            series.getData().add(new XYChart.Data<>(timeLabel, bid.getBidAmount()));
        }

        bidChart.getData().add(series);

        // Gán style CSS màu xanh dương cho đường tuyến tính trên đồ thị
        series.getNode().setStyle("-fx-stroke: #2563eb; -fx-stroke-width: 2;");
    }

    // COUNTDOWN

    /**
     * Khởi động chu kỳ thời gian 1 giây cập nhật đồng hồ đếm ngược, kích hoạt cảnh báo chống bắn tỉa giá (Anti-Snipe) khi thời gian dưới 60 giây.
     */
    private void startCountdown() {
        stopCountdown();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (currentAuction == null || currentAuction.getEndTime() == null) return;
            long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), currentAuction.getEndTime());
            if (seconds <= 0) {
                lblCdH.setText("00"); lblCdM.setText("00"); lblCdS.setText("00");
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

    /**
     * Ngắt dòng tiến trình Timeline đếm ngược để bảo vệ tài nguyên bộ nhớ Client.
     */
    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    // REALTIME SUBSCRIPTION (Observer Pattern)

    /**
     * Đăng ký mã kênh truyền tải real-time lên máy chủ Server để tiếp nhận các thông báo đẩy trong phòng chờ.
     */
    private void subscribeToUpdates() {
        new Thread(() -> {
            try {
                Request subReq = new Request(RequestType.SUBSCRIBE_AUCTION, auctionId);
                ClientSocketManager.getInstance().sendRequest(subReq);
                ClientSocketManager.getInstance().addObserver(this);
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố ngắt kết nối mạng khi gửi yêu cầu SUBSCRIBE_AUCTION", e);
            }
        }).start();
    }

    /**
     * Xử lý sự kiện mạng real-time đẩy về từ Server (Mẫu thiết kế Observer Pattern).
     * Phương thức này tự động thực thi bất đồng bộ trên luồng JavaFX UI Thread.
     */
    @Override
    public void onAuctionEvent(String event, String eventAuctionId, JsonObject payload) {
        if (!auctionId.equals(eventAuctionId)) return;

        switch (event) {
            case "BID_UPDATE" -> {
                double newPrice = payload.has("newPrice") ? payload.get("newPrice").getAsDouble() : 0;
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                if (currentAuction != null) currentAuction.setCurrentPrice(newPrice);
                loadBidHistory();
            }
            case "ANTI_SNIPE" -> {
                if (payload.has("newEndTime") && currentAuction != null) {
                    currentAuction.setEndTime(LocalDateTime.parse(payload.get("newEndTime").getAsString()));
                }
                paneAntiSnipe.setVisible(true);
                paneAntiSnipe.setManaged(true);
            }
            case "AUCTION_CLOSED" -> {
                stopCountdown();
                loadAuctionDetail();
            }
        }
    }

    // ACTION HANDLERS

    /**
     * Xử lý đóng gói và truyền tải chỉ thị đặt giá thủ công của người dùng lên hệ thống máy chủ.
     */
    @FXML
    void handlePlaceBid(ActionEvent event) {
        String amountStr = txtBidAmount.getText().trim();
        if (amountStr.isEmpty()) { showAlert("Lỗi", "Vui lòng nhập số tiền muốn đặt."); return; }
        try {
            double amount = Double.parseDouble(amountStr.replace(",", "").replace(".", ""));
            BidRequestDTO dto = new BidRequestDTO(auctionId, amount);
            Response res = ClientSocketManager.getInstance().sendRequest(new Request(RequestType.PLACE_BID, dto));
            if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                txtBidAmount.clear();
            } else {
                showAlert("Đặt giá thất bại", res != null ? res.getMessage() : "Lỗi kết nối");
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền không hợp lệ. Chỉ nhập số.");
        } catch (Exception e) {
            LOGGER.error("Gặp sự cố hệ thống khi thực hiện hành động đặt giá", e);
            showAlert("Lỗi", e.getMessage());
        }
    }

    /**
     * Đăng ký cấu hình hạn mức và bước giá tăng cho chức năng tự động đấu giá (Auto-Bid) lên Server.
     */
    @FXML
    void handleRegisterAutoBid(ActionEvent event) {
        String maxStr = txtAutoMax.getText().trim();
        String incStr = txtAutoInc.getText().trim();
        if (maxStr.isEmpty() || incStr.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đủ giá tối đa và bước tăng.");
            return;
        }
        try {
            double maxBid = Double.parseDouble(maxStr.replace(",", "").replace(".", ""));
            double incBid = Double.parseDouble(incStr.replace(",", "").replace(".", ""));

            AutoBidDTO dto = new AutoBidDTO(auctionId, maxBid, incBid);
            Response res = ClientSocketManager.getInstance().sendRequest(new Request(RequestType.REGISTER_AUTO_BID, dto));

            if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                paneAutoBidForm.setVisible(false); paneAutoBidForm.setManaged(false);
                paneAutoBidStatus.setVisible(true); paneAutoBidStatus.setManaged(true);
                lblAutoBidDetail.setText(String.format("Max: %,.0fđ | Inc: %,.0fđ", dto.getMaxBid(), dto.getIncrement()));
            } else {
                showAlert("Lỗi Auto-Bid", res != null ? res.getMessage() : "Lỗi kết nối");
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền không hợp lệ. Vui lòng chỉ nhập số (có thể dùng dấu phẩy hoặc chấm).");
        } catch (Exception e) {
            LOGGER.error("Gặp sự cố lỗi nghiệp vụ khi đăng ký tự động đặt giá", e);
            showAlert("Lỗi", String.format("Đã xảy ra lỗi: %s", e.getMessage()));
        }
    }

    /**
     * Gửi chỉ thị yêu cầu hủy bỏ cơ chế tự động đặt giá của tài khoản tại phiên hiện hành.
     */
    @FXML
    void handleCancelAutoBid(ActionEvent event) {
        try {
            Response res = ClientSocketManager.getInstance().sendRequest(new Request(RequestType.CANCEL_AUTO_BID, auctionId));
            if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                paneAutoBidStatus.setVisible(false); paneAutoBidStatus.setManaged(false);
                paneAutoBidForm.setVisible(true); paneAutoBidForm.setManaged(true);
            }
        } catch (Exception e) {
            LOGGER.error("Gặp lỗi mạng khi gửi chỉ thị hủy bỏ cơ chế Auto-Bid của phiên: {}", auctionId, e);
        }
    }

    /**
     * Thực hiện thu hồi tiến trình đếm ngược, hủy đăng ký cấu trúc lắng nghe Observer nhằm tránh rò rỉ bộ nhớ (Memory Leak) và quay lại trang danh sách.
     */
    @FXML
    void goBack() {
        stopCountdown();
        ClientSocketManager.getInstance().removeObserver(this);
        if (MainController.getInstance() != null) {
            MainController.getInstance().showPageList(null);
        }
    }

    /**
     * Đóng gói khởi tạo và hiển thị cấu trúc hộp thoại Pop-up thông báo cảnh báo chạy trên UI Thread nền FX.
     */
    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}