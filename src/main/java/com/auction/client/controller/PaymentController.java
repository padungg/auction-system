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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h2>PaymentController</h2>
 * <p>
 * Controller chịu trách nhiệm kiểm soát toàn bộ phân hệ Giao dịch, Hóa đơn và Tất toán tài chính (Payment Dashboard) trên ứng dụng Client.
 * </p>
 *
 * <p><b>Các đặc quyền nghiệp vụ cốt lõi:</b></p>
 * <ul>
 *   <li><b>Kiểm soát Tab giao diện (Tab Toggle Core):</b> Hoán đổi linh hoạt giữa danh mục hóa đơn chờ tất toán (Pending Payments) và lịch sử giao dịch thành công (Payment History).</li>
 *   <li><b>Dựng Layout động (Dynamic Component Factory):</b> Tự động phân tích mảng dữ liệu DTO nhận về từ mạng để sinh các thẻ đồ họa (Card UI) lồng nhau, gán badge phân loại mặt hàng, tính toán chi phí dịch vụ lũy tiến (2%) và cấu hình nút bấm tương tác tương ứng với hạn mức số dư hiện hành.</li>
 *   <li><b>Xử lý giao dịch luồng ngầm:</b> Đóng gói chỉ thị PAY_AUCTION truyền tải qua Socket kết nối mạng bất đồng bộ, tự động phát tín hiệu yêu cầu MainController đồng bộ lại số dư thực tế ngay khi tất toán thành công.</li>
 *   <li><b>Kiểm soát phân trang (Client-side Pagination Engine):</b> Vận hành bộ tính toán lát cắt danh sách chỉ mục (Sub-list Slice) để hiển thị bảng dữ liệu lịch sử không vượt ngưỡng khống chế dòng cố định.</li>
 * </ul>
 *
 * @since 1.0
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.model.dto.AuctionSummaryDTO
 */
public class PaymentController {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Chịu trách nhiệm giám sát tiến trình truyền nhận gói tin hóa đơn tài chính và cô lập sự cố lỗi IO mạng.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - DIỀU KHIỂN CHUYỂN TAB VÀ ĐIỀU PHỐI PANEL
    // =========================================================================
    @FXML
    private Button btnTabPending;
    @FXML
    private Button btnTabHistory;

    @FXML
    private VBox panelPending;
    @FXML
    private VBox panelHistory;
    @FXML
    private VBox cardContainer;
    @FXML
    private Label lblNoPending;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - LỊCH SỬ THANH TOÁN (HISTORY TABLE)
    // =========================================================================
    @FXML
    private TableView<AuctionSummaryDTO> tableHistory;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colHistName;
    @FXML
    private TableColumn<AuctionSummaryDTO, Double> colHistPrice;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colHistTime;
    @FXML
    private TableColumn<AuctionSummaryDTO, String> colHistStatus;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - BỘ ĐIỀU KHIỂN PHÂN TRANG (PAGINATION CONTROLS)
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
    // PARAMETERS VÀ BỘ NHỚ LƯU TRỮ PHÂN TRANG NỘI BỘ (PAGINATION STATES)
    // =========================================================================
    /** Số lượng dòng dữ liệu tối đa hiển thị trên một trang bảng lịch sử. */
    private static final int HIST_PAGE_SIZE = 10;

    /** Chỉ mục trang hiện tại trong cấu trúc phân trang dựa trên gốc số 0. */
    private int histCurrentPage = 0;

    /** Bộ lưu trữ bộ nhớ đệm RAM chứa toàn bộ danh sách hóa đơn trúng đấu giá đã tất toán. */
    private java.util.List<AuctionSummaryDTO> allHistory = new java.util.ArrayList<>();

    /** Danh sách quan sát (ObservableList) liên kết dữ liệu lịch sử hiển thị trực tiếp lên TableView. */
    private ObservableList<AuctionSummaryDTO> historyList = FXCollections.observableArrayList();

    /**
     * Phương thức vòng đời khởi tạo phân hệ giao diện (Lifecycle Hook).
     * Được tự động kích hoạt ngay sau khi cây phân cấp đồ họa của file FXML được tải thành công.
     * Cấu hình cấu trúc bảng, kích hoạt Worker Thread luồng ngầm đồng bộ hóa đơn chờ và lịch sử giao dịch.
     */
    @FXML
    public void initialize() {
        setupHistoryTable();
        loadPending();
        loadHistory();
    }

    /**
     * Định hình quy tắc liên kết thuộc tính DTO cho TableView lịch sử và tùy biến giao diện render Cell.
     * Áp dụng định dạng dấu phân tách phần nghìn cho trường tiền tệ VND, căn giữa ô dữ liệu,
     * gán nhãn Badge trạng thái "Đã thanh toán" cố định và khống chế chiều cao dòng tối ưu hiệu năng cuộn.
     */
    private void setupHistoryTable() {
        colHistName.setCellValueFactory(new PropertyValueFactory<>("itemName"));

        // Định dạng hiển thị cột số tiền tất toán giao dịch lịch sử
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colHistPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
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

        // Cấu hình Cell hiển thị nhãn đồ họa trạng thái hoàn thành mặc định của bảng lịch sử
        colHistStatus.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
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
        tableHistory.setFixedCellSize(50); // Khống chế kích thước chiều cao dòng cố định để tối ưu hóa bộ nhớ render đồ họa
    }

    /**
     * Điều hướng hoán đổi giao diện hiển thị sang Tab "Chờ thanh toán".
     * Xóa sạch style ghi đè trực tiếp, cấu hình lại các lớp CSS hoạt động động và quản lý không gian Layout Container.
     *
     * @param e Sự kiện Action kích hoạt từ nút bấm FXML
     */
    @FXML
    void switchTabPending(ActionEvent e) {
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
     * Điều hướng hoán đổi giao diện hiển thị sang Tab "Lịch sử tất toán".
     * Cập nhật các thuộc tính nhận diện đồ họa hoạt động và tái nạp danh sách dữ liệu.
     *
     * @param e Sự kiện Action kích hoạt từ nút bấm FXML
     */
    @FXML
    void switchTabHistory(ActionEvent e) {
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

    /**
     * Đính kèm lớp CSS phong cách hiển thị hoạt động (Active Style Class) cho nút chuyển Tab chỉ định.
     */
    private void setTabActive(Button btn) {
        btn.getStyleClass().remove("pay-tab-inactive");
        if (!btn.getStyleClass().contains("pay-tab-active")) {
            btn.getStyleClass().add("pay-tab-active");
        }
    }

    /**
     * Đính kèm lớp CSS phong cách hiển thị vô hiệu (Inactive Style Class) cho nút chuyển Tab chỉ định.
     */
    private void setTabInactive(Button btn) {
        btn.getStyleClass().remove("pay-tab-active");
        if (!btn.getStyleClass().contains("pay-tab-inactive")) {
            btn.getStyleClass().add("pay-tab-inactive");
        }
    }

    /**
     * Kích hoạt một tiến trình ngầm (Worker Thread) gửi yêu cầu mạng GET_PENDING_PAYMENTS qua Socket.
     * Tiếp nhận mảng danh sách các phiên trúng đấu giá đang chờ xử lý tất toán tài chính và đẩy về UI Thread.
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
                    Platform.runLater(() -> {
                        AlertUtils.showError("Lỗi kết nối", "Không thể lấy danh sách chờ thanh toán từ máy chủ!");
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi tải chờ thanh toán", e);
            }
        });
    }

    /**
     * Động cơ xây dựng cấu trúc danh sách thẻ (Card Layout Grid Factory).
     * Làm sạch Container cũ, kiểm tra điều kiện rỗng để bật nhãn cảnh báo khuyết dữ liệu,
     * trích xuất số dư an toàn hệ thống từ Session Cache phục vụ đối sánh hạn mức và sinh chuỗi thẻ đồ họa con.
     */
    private void buildPendingCards(AuctionSummaryDTO[] arr) {
        cardContainer.getChildren().clear(); // Giải phóng không gian hiển thị cũ
        if (arr == null || arr.length == 0) {
            lblNoPending.setVisible(true);
            lblNoPending.setManaged(true);
            return;
        }
        lblNoPending.setVisible(false);
        lblNoPending.setManaged(false);

        UserResponseDTO currentUser = SessionManager.getInstance().getCurrentUser();
        double userBalance = currentUser != null ? currentUser.getBalance() : 0;

        // Vòng lặp khởi tạo cấu trúc từng cấu trúc HBox thẻ thanh toán lồng nhau
        for (AuctionSummaryDTO dto : arr) {
            cardContainer.getChildren().add(buildCard(dto, userBalance));
        }
    }

    /**
     * Nhà máy sản xuất thành phần thẻ đồ họa phức hợp (Flyweight Compound Card Builder).
     * Khởi tạo cấu trúc Node cây phân cấp phức hợp (`HBox` -> `VBox`), phân bổ Emoji biểu tượng theo phân loại danh mục sản phẩm,
     * bóc tách chuỗi để dựng hệ thống nhãn thông số kỹ thuật, tính toán lũy tiến 2% chi phí dịch vụ hệ thống.
     * Định hình logic nút bấm chuyển đổi hành vi tương thích (Thanh toán ngay nếu đủ số dư hoặc Nạp tiền nếu thâm hụt số dư)
     * kèm nhãn văn bản cảnh báo chỉ số tiền thiếu cụ thể.
     *
     * @param dto         Đối tượng dữ liệu tóm tắt thông tin phiên đấu giá mục tiêu
     * @param userBalance Số dư ví tài khoản hiện hành trích xuất từ Session Cache
     * @return {@link HBox} Cấu trúc đồ họa hoàn chỉnh của Thẻ hóa đơn chờ thanh toán
     */
    private HBox buildCard(AuctionSummaryDTO dto, double userBalance) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("pay-card");

        // Cấu hình nhãn biểu tượng đại diện căn cứ theo danh mục phân loại mặt hàng sản phẩm
        Label icon = new Label("🖥");
        icon.getStyleClass().add("pay-icon");
        if ("ART".equalsIgnoreCase(dto.getItemType()))
            icon.setText("🎨");
        else if ("VEHICLE".equalsIgnoreCase(dto.getItemType()))
            icon.setText("🚗");

        // Khởi tạo khối bố cục chứa văn bản thông tin hóa đơn (Left Inner Column)
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(dto.getItemName() != null ? dto.getItemName() : "Không rõ");
        nameLabel.getStyleClass().add("pay-name-label");

        // Đồng bộ nhãn chữ tiếng Việt và áp dụng lớp CSS phong cách đồ họa đặc thù cho Badge danh mục mặt hàng
        String itemType = dto.getItemType();
        String displayType = "";
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

        // Khởi tạo khối thông tin cảnh báo thời hạn tất toán hóa đơn bắt buộc
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

        // Khởi tạo khối bố cục hạch toán tài chính và tích hợp nút tương tác bấm (Right Inner Column)
        double price = dto.getCurrentPrice();
        double fee = price * 0.02; // Cách tính chi phí dịch vụ cố định 2% theo cấu trúc hệ thống công thức tài chính
        double total = price + fee;
        boolean canPay = userBalance >= total; // Đối chiếu hạn mức xác thực điều kiện thanh toán lập tức

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

        // Đính kèm bộ lắng nghe sự kiện nút xem sản phẩm (Chuyển tiếp view về phòng chi tiết sản phẩm con)
        Button btnView = new Button("🔍 Xem sản phẩm");
        btnView.getStyleClass().add("pay-btn-view");
        btnView.setOnAction(e -> MainController.getInstance().openAuctionDetail(dto.getAuctionId()));

        // Thiết lập cấu hình nút lệnh thanh toán: Biến đổi nhãn văn bản và đính kèm class phong cách tương thích số dư
        Button btnPay = new Button("💳 Nạp tiền & Thanh toán");
        if (canPay) {
            btnPay.setText("💳 Thanh toán ngay");
            btnPay.getStyleClass().add("pay-btn-pay-now");
        } else {
            btnPay.getStyleClass().add("pay-btn-pay-insufficient");
        }
        btnPay.setOnAction(e -> handlePayForAuction(dto));

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(btnView, btnPay);

        // Đính kèm nhãn văn bản cảnh báo và tính toán chỉ số số dư thâm hụt cụ thể nếu canPay = False
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
     * Đóng gói mã chỉ thị PAY_AUCTION truyền tải gói tin qua Socket hệ thống luồng ngầm để kết chuyển hóa đơn.
     * Nhận phản hồi mạng thành công, phát lệnh thông báo Popup, thực hiện nạp làm mới tập dữ liệu các danh mục Tab
     * và phát tín hiệu ép buộc MainController đồng bộ kéo lại số dư ví thực tế từ Database máy chủ.
     *
     * @param dto Đối tượng dữ liệu tóm tắt phiên đấu giá cần thực thi lệnh chi trả tất toán
     */
    private void handlePayForAuction(AuctionSummaryDTO dto) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.PAY_AUCTION, dto.getAuctionId());
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                // Trả luồng xử lý và kết xuất thông điệp về JavaFX Application Thread an toàn luồng đồ họa
                Platform.runLater(() -> {
                    if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thanh toán thành công! 🎉",
                                "Bạn đã thanh toán thành công cho sản phẩm: " + dto.getItemName());
                        loadPending(); // Làm mới hiển thị vùng tab hóa đơn chờ
                        loadHistory(); // Tái nạp danh mục lịch sử giao dịch thành công
                        if (MainController.getInstance() != null) {
                            MainController.getInstance().refreshBalanceFromServer(); // Ép đồng bộ ngược số dư lên Header hệ thống
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
     * Kích hoạt luồng Worker Thread ngầm truyền yêu cầu GET_PAYMENT_HISTORY tới Server.
     * Tiếp nhận mảng danh sách bản ghi tóm tắt hóa đơn đã chi trả thành công và thiết lập chỉ mục phân trang gốc.
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
                        if (arr != null)
                            java.util.Collections.addAll(allHistory, arr);
                        histCurrentPage = 0; // Đặt chỉ mục trang mặc định là 0 khi nạp mới từ máy chủ
                        renderHistPage();
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Lỗi tải lịch sử thanh toán", e);
            }
        });
    }

    // =========================================================================
    // ĐỘNG CƠ THUẬT TOÁN PHÂN TRANG GIAO DIỆN (PAGINATION LOGIC ENGINE)
    // =========================================================================

    /**
     * Thuật toán bóc tách phân mảnh phân trang cục bộ phía Client (Client-side Page Slicing Calculus).
     * Tính toán chỉ mục biên giới dạt cắt `from` và `to` dựa trên hằng số kích thước trang để làm mới danh sách `historyList`,
     * chuẩn hóa văn bản nhãn hướng dẫn trực quan, kiểm soát trạng thái vô hiệu hóa (Disabled State) của tập hợp nút bấm biên giới
     * và sinh chuỗi nút chọn trang số động dạng thanh trượt (Slider) giới hạn vùng hiển thị đồng thời tối đa 5 nút bấm số trang.
     */
    private void renderHistPage() {
        int total = allHistory.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / HIST_PAGE_SIZE));
        int from = histCurrentPage * HIST_PAGE_SIZE;
        int to = Math.min(from + HIST_PAGE_SIZE, total);

        historyList.clear();
        historyList.addAll(allHistory.subList(from, to)); // Trích xuất phân mảnh dữ liệu (Sub-list slice) lưu trong RAM hiển thị lên bảng

        if (histPageInfo != null)
            histPageInfo.setText("Trang " + (histCurrentPage + 1) + " / " + totalPages
                    + "  (" + total + " giao dịch)");

        // Đồng bộ hóa trạng thái vô hiệu hóa của các nút bấm chuyển dịch nhanh đầu cuối dựa vào vị trí biên chỉ mục
        if (histBtnFirst != null)
            histBtnFirst.setDisable(histCurrentPage == 0);
        if (histBtnPrev != null)
            histBtnPrev.setDisable(histCurrentPage == 0);
        if (histBtnNext != null)
            histBtnNext.setDisable(histCurrentPage >= totalPages - 1);
        if (histBtnLast != null)
            histBtnLast.setDisable(histCurrentPage >= totalPages - 1);

        // Khởi tạo và kết xuất thanh cấu trúc nhóm nút bấm trang số động tương tác liền kề
        if (histPageBox != null) {
            histPageBox.getChildren().clear();
            int maxBtn = 5, startP = Math.max(0, histCurrentPage - 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn)
                startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                Button btn = new Button(String.valueOf(p + 1));
                btn.getStyleClass().add(p == histCurrentPage
                        ? "pay-pagination-active"
                        : "pay-pagination-inactive");
                btn.setOnAction(e -> {
                    histCurrentPage = pg;
                    renderHistPage();
                });
                histPageBox.getChildren().add(btn);
            }
        }
    }

    /**
     * Nhảy về vị trí trang đầu tiên trong danh mục phân trang lịch sử giao dịch.
     * @param e Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void histGoFirst(ActionEvent e) {
        histCurrentPage = 0;
        renderHistPage();
    }

    /**
     * Di chuyển lùi lại một trang trong danh mục phân trang lịch sử giao dịch (nếu hợp lệ).
     * @param e Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void histGoPrev(ActionEvent e) {
        if (histCurrentPage > 0) {
            histCurrentPage--;
            renderHistPage();
        }
    }

    /**
     * Di chuyển tiến lên một trang kế tiếp trong danh mục phân trang lịch sử giao dịch (nếu hợp lệ).
     * @param e Sự kiện Action gửi từ giao diện người dùng FXML
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
     * Nhảy thẳng tới vị trí trang cuối cùng trong danh mục phân trang lịch sử giao dịch.
     * @param e Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    public void histGoLast(ActionEvent e) {
        histCurrentPage = Math.max(0, (int) Math.ceil((double) allHistory.size() / HIST_PAGE_SIZE) - 1);
        renderHistPage();
    }
}