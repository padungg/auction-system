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
 * <h2>AuctionDetailController</h2>
 * <p>
 * Controller điều phối phân hệ Chi tiết phiên đấu giá (Auction Detail View) trên ứng dụng Client.
 * </p>
 *
 * <p><b>Các chức năng nghiệp vụ cốt lõi:</b></p>
 * <ul>
 *   <li><b>Đồng bộ thời gian thực:</b> Tích hợp mẫu thiết kế Observer Pattern tiếp nhận gói tin đẩy (Push Notifications) về biến động giá (Bid Update), thời gian bù chống bắn tỉa (Anti-Snipe) và đóng phiên.</li>
 *   <li><b>Kiểm soát luồng Timeline:</b> Vận hành cơ chế đếm ngược (Countdown Engine) chính xác theo giây và tự động giải phóng tài nguyên luồng khi chuyển view.</li>
 *   <li><b>Trực quan hóa dữ liệu:</b> Kết xuất biểu đồ tuyến tính LineChart mô tả tiến trình nhảy giá sản phẩm và phân bổ lưới thông số kỹ thuật động.</li>
 *   <li><b>Quản lý giao dịch:</b> Kiểm soát quy trình xác thực hạn mức đặt giá thủ công và đăng ký/hủy bỏ cấu hình đấu giá tự động (Auto-Bid Engine).</li>
 * </ul>
 *
 * @since 1.0
 * @see com.auction.client.observer.AuctionEventObserver
 * @see com.auction.client.network.ClientSocketManager
 */
public class AuctionDetailController implements AuctionEventObserver {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo cơ chế an toàn đa luồng (Thread-safe).
     * Phục vụ ghi vết vòng đời kết nối WebSocket, cô lập ngoại lệ mạng IO và theo dõi lịch sử đặt giá.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDetailController.class);

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - THÔNG TIN CHI TIẾT SẢN PHẨM (LEFT PANEL)
    // =========================================================================
    @FXML
    private Label lblBreadcrumbName, lblDetailName, lblDetailStatus, lblDetailEmoji, lblDetailDesc;
    @FXML
    private javafx.scene.image.ImageView imgProduct;
    @FXML
    private GridPane gridSpecs;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - GIÁ CẢ & ĐẾM NGƯỢC THỜI GIAN (RIGHT PANEL)
    // =========================================================================
    @FXML
    private Label lblCurrentPrice, lblStartPrice, lblBidStep;
    @FXML
    private Label lblCdH, lblCdM, lblCdS;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - KHỐI CHỨC NĂNG ĐIỀU KHIỂN (CONTROLS PANEL)
    // =========================================================================
    @FXML
    private HBox paneAntiSnipe;
    @FXML
    private VBox paneWinner, paneBidForm, paneAutoBidStatus, paneAutoBidForm;
    @FXML
    private Label lblWinnerName, lblAutoBidDetail;

    // Các trường nhập liệu thông tin biểu mẫu đặt giá
    @FXML
    private TextField txtBidAmount, txtAutoMax, txtAutoInc;

    // Bảng dữ liệu lịch sử các lượt đặt giá (Bid History Table)
    @FXML
    private TableView<BidTransaction> tableHistory;
    @FXML
    private TableColumn<BidTransaction, String> colUser;
    @FXML
    private TableColumn<BidTransaction, Double> colAmount;
    @FXML
    private TableColumn<BidTransaction, LocalDateTime> colTime;

    // Thành phần đồ họa trực quan hóa biến động giá (LineChart)
    @FXML
    private LineChart<String, Number> bidChart;
    @FXML
    private CategoryAxis chartXAxis;
    @FXML
    private NumberAxis chartYAxis;

    // =========================================================================
    // CẤU TRÚC LƯU TRỮ VÀ THAM SỐ NỘI BỘ (INTERNAL DATA STATES)
    // =========================================================================
    /** Mã định danh duy nhất của phiên đấu giá hiện hành. */
    private String auctionId;

    /** Đối tượng DTO chứa toàn bộ dữ liệu chi tiết của phiên đấu giá được đồng bộ. */
    private AuctionDetailDTO currentAuction;

    /** Trình quản lý luồng thời gian (JavaFX Timeline) kiểm soát tác vụ đếm ngược. */
    private Timeline countdownTimeline;

    /** Danh sách quan sát (ObservableList) liên kết dữ liệu lịch sử đặt giá trực tiếp lên bảng. */
    private final ObservableList<BidTransaction> bidHistory = FXCollections.observableArrayList();

    // =========================================================================
    // KHỞI TẠO CẤU HÌNH BAN ĐẦU (INITIALIZATION METHODS)
    // =========================================================================

    /**
     * Phương thức thiết lập và nạp dữ liệu ban đầu cho phân hệ chi tiết phiên đấu giá.
     * Được kích hoạt thủ công từ Controller chuyển tiếp khi truyền định danh phiên.
     *
     * @param auctionId Mã định danh duy nhất của phiên đấu giá mục tiêu
     */
    public void initData(String auctionId) {
        this.auctionId = auctionId;
        setupTable();
        setupChart();
        loadAuctionDetail();
        subscribeToUpdates();
    }

    /**
     * Định hình cấu trúc phân mảnh dữ liệu bảng Lịch sử đặt giá và định nghĩa quy tắc render cell.
     * Tự động đính kèm Badge đồ họa đại diện cho các lượt đặt giá sinh ra từ hệ thống tự động (Auto-Bid Engine)
     * và áp dụng class CSS làm nổi bật giá đặt cao nhất tại dòng đầu tiên.
     */
    private void setupTable() {
        // Cấu hình Cell hiển thị cột định danh tài khoản người đặt kèm ký hiệu AUTO nếu có
        colUser.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
        colUser.setCellFactory(tc -> new TableCell<>() {
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

        // Cấu hình Cell hiển thị số tiền đặt giá: Thêm dấu phân cách phần nghìn và highlight dòng dẫn đầu
        colAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    getStyleClass().remove("auc-detail-first-row-price");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    // Áp dụng lớp CSS đặc thù làm nổi bật mức giá cao nhất hiện tại (dòng chỉ mục số 0)
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

        // Cấu hình Cell hiển thị mốc thời gian: Định dạng phân tách dòng giữa Giờ và Ngày
        colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colTime.setCellFactory(tc -> new TableCell<>() {
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
     * Cấu hình thiết lập đồ họa nền tảng và gán các thuộc tính tối ưu hiệu năng hiển thị cho LineChart.
     */
    private void setupChart() {
        bidChart.setCreateSymbols(true);
        bidChart.setAnimated(false); // Vô hiệu hóa hiệu ứng chuyển động để tăng tốc độ vẽ đồ thị thời gian thực
        bidChart.setLegendVisible(false);
        bidChart.getStyleClass().add("auc-detail-chart-bg");
        chartXAxis.setLabel("");
        chartXAxis.setTickLabelRotation(0);
        chartYAxis.setLabel("");
        chartYAxis.setForceZeroInRange(false); // Cho phép trục tung tự động co giãn biên độ linh hoạt theo giá trị tiền tệ
    }

    // =========================================================================
    // ĐỒNG BỘ VÀ TẢI DỮ LIỆU TỪ MẠNG (NETWORK IO OPERATIONS)
    // =========================================================================

    /**
     * Kích hoạt một nhiệm vụ luồng ngầm (Worker Thread) gửi yêu cầu truy vấn cấu trúc
     * dữ liệu chi tiết của phiên đấu giá hiện tại từ máy chủ về thiết bị Client.
     */
    private void loadAuctionDetail() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_AUCTION_DETAIL, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    currentAuction = res.getPayloadAs(AuctionDetailDTO.class);
                    Platform.runLater(this::updateUI);
                    loadBidHistory(); // Chuyển tiếp tiến trình kéo lịch sử đặt giá liên đới
                } else {
                    Platform.runLater(() -> com.auction.client.util.AlertUtils.showError("Lỗi kết nối",
                            "Không thể nạp chi tiết phiên đấu giá từ server."));
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố khi nạp thông tin chi tiết phiên đấu giá từ máy chủ", e);
                Platform.runLater(() -> com.auction.client.util.AlertUtils.showError("Lỗi hệ thống",
                        "Lỗi kết nối máy chủ: " + e.getMessage()));
            }
        });
    }

    /**
     * Gửi yêu cầu mạng truy vấn toàn bộ tập hợp các bản ghi lịch sử giao dịch đặt giá đã thực thi của phiên đấu giá.
     * Cập nhật danh sách quan sát TableView và tái tạo đường tuyến tính biến động trên đồ thị biểu đồ.
     */
    private void loadBidHistory() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_BID_HISTORY, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    List<BidTransaction> history = res.getPayloadAs(new TypeToken<List<BidTransaction>>() {
                    });
                    Platform.runLater(() -> {
                        bidHistory.setAll(history);
                        updateChart(history); // Đẩy tập dữ liệu sang công cụ vẽ đồ thị
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố khi tải lịch sử đặt giá từ máy chủ", e);
            }
        });
    }

    // =========================================================================
    // CẬP NHẬT THÀNH PHẦN GIAO DIỆN (UI RENDERING OPERATIONS)
    // =========================================================================

    /**
     * Phân phối toàn bộ thuộc tính dữ liệu từ đối tượng DTO nguồn lên hệ thống các nút nhãn hiển thị JavaFX đầu cuối.
     * Thực hiện bóc tách luồng ảnh Base64, đồng bộ Badge trạng thái, kiểm soát ẩn hiện khối người chiến thắng và khởi tạo bảng lưới.
     */
    private void updateUI() {
        if (currentAuction == null)
            return;

        // Đồng bộ dữ liệu tiêu đề và khối văn bản mô tả sản phẩm
        lblDetailName.setText(currentAuction.getItemName());
        lblBreadcrumbName.setText(currentAuction.getItemName());
        lblDetailDesc
                .setText(currentAuction.getDescription() != null ? currentAuction.getDescription() : "Không có mô tả.");

        // Tiến trình giải mã luồng văn bản Base64 thành cấu trúc ảnh nhị phân hiển thị lên ImageView
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

        // Định dạng text tiền tệ hiển thị cho các nhãn thông số tài chính
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentAuction.getCurrentPrice()));
        lblStartPrice.setText(String.format("Giá khởi điểm: %,.0f VNĐ", currentAuction.getStartingPrice()));
        lblBidStep.setText(String.format("Bước giá tối thiểu: %,.0f VNĐ", currentAuction.getStepPrice()));

        // Tái xác định lớp CSS phong cách riêng cho Badge trạng thái phiên
        String status = currentAuction.getStatus();
        updateStatusBadge(status);

        // Ánh xạ từ khóa danh mục mặt hàng sang biểu tượng Emoji dự phòng trực quan
        updateEmoji(currentAuction.getItemDetails());

        // Kiểm soát cơ chế quản lý không gian hiển thị của các khối chức năng theo trạng thái vòng đời
        boolean isRunning = "RUNNING".equalsIgnoreCase(status);
        boolean isFinished = "FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);

        paneBidForm.setVisible(isRunning);
        paneBidForm.setManaged(isRunning);

        paneWinner.setVisible(isFinished && currentAuction.getCurrentWinnerName() != null);
        paneWinner.setManaged(isFinished && currentAuction.getCurrentWinnerName() != null);
        if (isFinished && currentAuction.getCurrentWinnerName() != null) {
            lblWinnerName.setText(currentAuction.getCurrentWinnerName());
        }

        // Kiểm soát điều khiển kích hoạt hoặc dừng khẩn cấp bộ đếm ngược
        if (isRunning) {
            startCountdown();
        } else {
            stopCountdown();
        }

        // Tách cấu trúc văn bản để xây dựng lưới thông số kỹ thuật sản phẩm
        buildSpecsGrid();
    }

    /**
     * Cập nhật nhãn chữ hiển thị và dọn dẹp, tái cấu trúc class định dạng màu sắc CSS đặc trưng cho từng trạng thái.
     */
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

    /**
     * Phân tích ngữ nghĩa từ khóa có trong chi tiết thuộc tính sản phẩm nhằm gán mã đồ họa Emoji phù hợp.
     */
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

    /**
     * Thuật toán phân tích và bóc tách cấu trúc văn bản định dạng cặp khóa-giá trị phân tách bởi dấu hai chấm (Key:Value).
     * Sinh tự động các cặp Label và phân bổ không gian vào lưới GridPane một cách có hệ thống.
     */
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

    /**
     * Tái tạo tập dữ liệu đồ thị đại diện (XYChart.Series) từ mảng danh sách lịch sử nhằm mô tả trực quan
     * tuyến tính xu hướng biến động bước giá đấu, gán nhãn mốc thời gian trục hoành.
     */
    private void updateChart(List<BidTransaction> history) {
        bidChart.getData().clear();
        if (history == null || history.isEmpty())
            return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá đấu");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (int i = 0; i < history.size(); i++) {
            BidTransaction bid = history.get(i);
            String timeLabel = bid.getBidTime() != null ? bid.getBidTime().format(fmt) : "?";
            series.getData().add(new XYChart.Data<>(timeLabel, bid.getBidAmount()));
        }

        bidChart.getData().add(series);

        // Gán bộ định dạng lớp CSS tùy biến cho nút liên kết đồ thị tuyến tính
        if (series.getNode() != null) {
            series.getNode().getStyleClass().add("bid-chart-series");
        }
    }

    // =========================================================================
    // CƠ CHẾ ĐẾM NGƯỢC THỜI GIAN (COUNTDOWN ENGINE)
    // =========================================================================

    /**
     * Khởi tạo và kích hoạt bộ quản lý thời gian JavaFX Timeline thực hiện chu kỳ lặp vô hạn tần suất 1 giây.
     * Tính toán khoảng cách ChronoUnit để cập nhật đồng hồ hiển thị, đồng thời tự động kích hoạt cảnh báo
     * trạng thái bù giờ chống bắn tỉa giá (Anti-Snipe) khi thời lượng còn lại ít hơn 60 giây.
     */
    private void startCountdown() {
        stopCountdown();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (currentAuction == null || currentAuction.getEndTime() == null)
                return;
            long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), currentAuction.getEndTime());
            if (seconds <= 0) {
                lblCdH.setText("00");
                lblCdM.setText("00");
                lblCdS.setText("00");
                stopCountdown();
                loadAuctionDetail(); // Phát tín hiệu tải lại để đồng bộ trạng thái đóng phiên từ Server
                return;
            }
            lblCdH.setText(String.format("%02d", seconds / 3600));
            lblCdM.setText(String.format("%02d", (seconds % 3600) / 60));
            lblCdS.setText(String.format("%02d", seconds % 60));

            // Hiển thị panel thông báo trạng thái Anti-Snipe nếu thời gian đếm ngược chạm biên dưới 1 phút
            if (seconds < 60) {
                paneAntiSnipe.setVisible(true);
                paneAntiSnipe.setManaged(true);
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    /**
     * Hủy bỏ luồng Timeline đếm ngược hiện hành nhằm mục đích giải phóng tài nguyên CPU
     * và ngăn chặn triệt để hiện tượng rò rỉ bộ nhớ (Memory Leak) hệ thống Client.
     */
    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN ĐẨY THỜI GIAN THỰC (OBSERVER PATTERN IMPLEMENTATION)
    // =========================================================================

    /**
     * Đóng gói mã chỉ thị SUBSCRIBE_AUCTION gửi lên phía hệ thống Server nhằm ghi nhận tài khoản hiện tại
     * đăng ký vào hàng đợi lắng nghe các sự kiện phát sinh bên trong phòng phiên đấu giá cụ thể này.
     */
    private void subscribeToUpdates() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request subReq = new Request(RequestType.SUBSCRIBE_AUCTION, auctionId);
                ClientSocketManager.getInstance().sendRequest(subReq);
                ClientSocketManager.getInstance().addObserver(this); // Đăng ký thực thể hiện hành làm Observer
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố ngắt kết nối mạng khi gửi yêu cầu SUBSCRIBE_AUCTION", e);
            }
        });
    }

    /**
     * Phương thức phản hồi sự kiện đẩy mạng (Callback Method) nhận diện từ thiết kế mẫu Observer Pattern.
     * Chuyển tiếp tiến trình thực thi bất đồng bộ an toàn về JavaFX Application Thread để cập nhật giao diện.
     *
     * @param event          Tên mã định danh sự kiện (BID_UPDATE, ANTI_SNIPE, AUCTION_CLOSED)
     * @param eventAuctionId Mã định danh phiên phát sinh sự kiện
     * @param payload        Đối tượng dữ liệu Json chứa thông số chi tiết đính kèm từ Server
     */
    @Override
    public void onAuctionEvent(String event, String eventAuctionId, JsonObject payload) {
        if (!auctionId.equals(eventAuctionId))
            return;

        switch (event) {
            case "BID_UPDATE" -> {
                double newPrice = payload.has("newPrice") ? payload.get("newPrice").getAsDouble() : 0;
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                if (currentAuction != null)
                    currentAuction.setCurrentPrice(newPrice);
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

    // =========================================================================
    // ĐÓN NHẬN SỰ KIỆN TƯƠNG TÁC (UI ACTION HANDLERS)
    // =========================================================================

    /**
     * Đón nhận hành động xử lý Đặt giá thủ công (Manual Place Bid) từ giao diện người dùng.
     * Thực hiện bóc tách định dạng ký tự số, xác thực biên độ kiểm tra bước giá tối thiểu bắt buộc
     * dựa theo vị thế lượt đấu (Lượt đầu tiên so với giá sàn hoặc lượt kế thừa so với giá hiện tại + bước giá).
     *
     * @param event Sự kiện kích hoạt hành động nút bấm từ giao diện UI
     */
    @FXML
    void handlePlaceBid(ActionEvent event) {
        String amountStr = txtBidAmount.getText().trim();
        if (amountStr.isEmpty()) {
            com.auction.client.util.AlertUtils.showWarning("Lỗi", "Vui lòng nhập số tiền muốn đặt.");
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr.replace(",", "").replace(".", ""));

            double minRequiredBid;
            boolean isFirstBid = bidHistory.isEmpty();
            if (isFirstBid) {
                minRequiredBid = currentAuction.getStartingPrice();
                if (amount < minRequiredBid) {
                    com.auction.client.util.AlertUtils.showWarning("Lỗi đặt giá",
                            "Giá đặt phải lớn hơn hoặc bằng giá khởi điểm: "
                                    + String.format("%,.0f VNĐ", minRequiredBid));
                    return;
                }
            } else {
                minRequiredBid = currentAuction.getCurrentPrice() + currentAuction.getStepPrice();
                if (amount < minRequiredBid) {
                    com.auction.client.util.AlertUtils.showWarning("Lỗi đặt giá",
                            "Giá đặt tối thiểu phải là " + String.format("%,.0f VNĐ", minRequiredBid)
                                    + " (Giá hiện tại + Bước giá tối thiểu "
                                    + String.format("%,.0f VNĐ", currentAuction.getStepPrice()) + ")");
                    return;
                }
            }

            BidRequestDTO dto = new BidRequestDTO(auctionId, amount);
            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Response res = ClientSocketManager.getInstance()
                            .sendRequest(new Request(RequestType.PLACE_BID, dto));
                    Platform.runLater(() -> {
                        if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                            txtBidAmount.clear();
                        } else {
                            com.auction.client.util.AlertUtils.showWarning("Đặt giá thất bại",
                                    res != null ? res.getMessage() : "Lỗi kết nối");
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố hệ thống khi thực hiện hành động đặt giá", e);
                    Platform.runLater(() -> com.auction.client.util.AlertUtils.showError("Lỗi", e.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            com.auction.client.util.AlertUtils.showWarning("Lỗi", "Số tiền không hợp lệ. Chỉ nhập số.");
        }
    }

    /**
     * Đón nhận hành động đăng ký cấu hình Đấu giá tự động (Register Auto-Bid) từ giao diện UI.
     * Thu thập thông tin hạn mức trần tối đa (Max Bid) và bước giá tăng lũy tiến (Increment),
     * gửi gói tin REGISTER_AUTO_BID ngầm và chuyển đổi trạng thái hiển thị giao diện sang khối giám sát.
     *
     * @param event Sự kiện kích hoạt hành động nút bấm từ giao diện UI
     */
    @FXML
    void handleRegisterAutoBid(ActionEvent event) {
        String maxStr = txtAutoMax.getText().trim();
        String incStr = txtAutoInc.getText().trim();
        if (maxStr.isEmpty() || incStr.isEmpty()) {
            com.auction.client.util.AlertUtils.showWarning("Lỗi", "Vui lòng nhập đủ giá tối đa và bước tăng.");
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
                            com.auction.client.util.AlertUtils.showWarning("Lỗi Tự động đặt giá",
                                    res != null ? res.getMessage() : "Lỗi kết nối");
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố lỗi nghiệp vụ khi đăng ký tự động đặt giá", e);
                    Platform.runLater(() -> com.auction.client.util.AlertUtils.showError("Lỗi",
                            String.format("Đã xảy ra lỗi: %s", e.getMessage())));
                }
            });
        } catch (NumberFormatException e) {
            com.auction.client.util.AlertUtils.showWarning("Lỗi",
                    "Số tiền không hợp lệ. Vui lòng chỉ nhập số (có thể dùng dấu phẩy hoặc chấm).");
        }
    }

    /**
     * Đón nhận hành động Hủy bỏ chế độ đấu giá tự động (Cancel Auto-Bid) từ giao diện người dùng.
     * Gửi gói tin chỉ thị CANCEL_AUTO_BID lên Server và khôi phục lại form cấu hình nhập liệu ban đầu.
     *
     * @param event Sự kiện kích hoạt hành động nút bấm từ giao diện UI
     */
    @FXML
    void handleCancelAutoBid(ActionEvent event) {
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

    /**
     * Thực hiện chuỗi tác vụ đóng cửa sổ quay lại màn hình danh mục chính.
     * Ngắt Timeline đếm ngược và xóa thực thể khỏi danh sách đăng ký Observer Pattern
     * nhằm bảo vệ tài nguyên hệ thống Client trước khi kích hoạt chuyển đổi trang.
     */
    @FXML
    public void goBack() {
        stopCountdown();
        ClientSocketManager.getInstance().removeObserver(this); // Hủy đăng ký cấu trúc để giải phóng bộ nhớ RAM
        if (MainController.getInstance() != null) {
            MainController.getInstance().showPageList(null);
        }
    }
}