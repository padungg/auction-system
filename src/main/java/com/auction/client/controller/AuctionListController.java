package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * <h2>AuctionListController</h2>
 * <p>
 * Controller chịu trách nhiệm điều phối phân hệ Danh sách đấu giá công khai (Auction List Dashboard) trên Client.
 * </p>
 *
 * <p><b>Các nghiệp vụ tích hợp cốt lõi:</b></p>
 * <ul>
 *   <li><b>Thống kê định lượng:</b> Tổng hợp và kết xuất thời gian thực các chỉ số đo lường (KPI) của toàn hệ thống lên Dashboard.</li>
 *   <li><b>Bộ lọc đa tiêu chí:</b> Sàng lọc động danh sách theo danh mục phân loại mặt hàng (Category) và trạng thái vòng đời phiên đấu giá (Status).</li>
 *   <li><b>Tìm kiếm thời gian thực:</b> Lắng nghe thay đổi thuộc tính văn bản (Text Property Listener) để thực thi mệnh đề tìm kiếm lập tức.</li>
 *   <li><b>Tối ưu hóa UI hiệu năng cao:</b> Ứng dụng cấu trúc lưu trữ Flyweight Pattern thông qua bộ nhớ đệm `cardCacheMap`,
 *   ngăn ngừa rò rỉ bộ nhớ (Memory Leak) và giảm thiểu chi phí I/O khi nạp lại tệp tin thiết kế FXML.</li>
 * </ul>
 *
 * @since 1.0
 * @see javafx.fxml.Initializable
 * @see com.auction.client.network.ClientSocketManager
 */
public class AuctionListController implements Initializable {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Chịu trách nhiệm lưu vết trạng thái đồng bộ IO, các cảnh báo mạng mạng và ngoại lệ biên dịch tài nguyên UI.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionListController.class);

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - KHỐI THỐNG KÊ & TÌM KIẾM (STATS & SEARCH)
    // =========================================================================
    @FXML private Label statTotal, statActive, statBids, statFinished;
    @FXML private TextField txtSearch;
    @FXML private FlowPane auctionGrid;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - THANH ĐIỀU HƯỚNG BỘ LỌC (FILTER CONTROLS)
    // =========================================================================
    @FXML private Button filterAll, filterElectronics, filterArt, filterVehicle;
    @FXML private Button statusAll, statusRunning, statusOpen, statusFinished;

    // =========================================================================
    // BIẾN QUẢN LÝ TIẾN TRÌNH NỘI BỘ (INTERNAL DATA STATES)
    // =========================================================================
    /** Bộ lưu trữ RAM chứa toàn bộ danh sách tóm tắt phiên đấu giá kéo về từ Server. */
    private List<AuctionSummaryDTO> allAuctions = new ArrayList<>();

    /** Tham số lưu giữ tiêu chí lọc danh mục mặt hàng hiện tại (Mặc định: all). */
    private String currentCategory = "all";

    /** Tham số lưu giữ tiêu chí lọc trạng thái phiên hiện tại (Mặc định: all). */
    private String currentStatus = "all";

    /**
     * Phương thức khởi tạo vòng đời JavaFX View (Lifecycle Hook).
     * Thiết lập khởi chạy tiến trình kết nối luồng mạng và đính kèm bộ lắng nghe thay đổi (Listener)
     * trên trường nhập liệu tìm kiếm nhằm kích hoạt chuỗi lọc động thời gian thực.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadAuctions();

        // Đính kèm bộ giám sát thuộc tính văn bản phục vụ tìm kiếm động ngay khi gõ phím
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    /**
     * Kích hoạt một tiến trình nền độc lập (Worker Thread) truyền chỉ thị GET_ALL_AUCTIONS qua kết nối Socket.
     * Tiếp nhận mảng dữ liệu phản hồi, cô lập lỗi mạng bảo vệ trạng thái an toàn hệ thống,
     * và chuyển tiếp đồng bộ về JavaFX Application Thread nhằm làm mới dữ liệu giao diện.
     */
    private void loadAuctions() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    allAuctions = res.getPayloadAs(new TypeToken<List<AuctionSummaryDTO>>(){});
                } else {
                    allAuctions = new ArrayList<>();
                    LOGGER.warn("Không tải được dữ liệu từ DB, hiển thị danh sách rỗng.");
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố kết nối lỗi mạng khi thực thi yêu cầu GET_ALL_AUCTIONS", e);
                allAuctions = new ArrayList<>();
            }
            // Chuyển tiếp tiến trình xử lý đồ họa về luồng chính UI của JavaFX tránh xung đột luồng
            Platform.runLater(() -> {
                cardCacheMap.clear(); // Giải phóng hoàn toàn bộ nhớ đệm cache khi nạp lại từ máy chủ để tránh rò rỉ RAM và cập nhật các Node mới nhất
                updateStats();
                applyFilters();
            });
        });
    }

    /**
     * Phân tích, tổng hợp luồng dữ liệu RAM thông qua cơ chế Stream API để bóc tách
     * số lượng phiên, lượt đặt giá và cập nhật trực tiếp lên các nhãn văn bản thống kê trên View.
     */
    private void updateStats() {
        statTotal.setText(String.valueOf(allAuctions.size()));
        long runningCount = allAuctions.stream().filter(a -> "RUNNING".equalsIgnoreCase(a.getStatus())).count();
        statActive.setText(String.valueOf(runningCount));

        long finishedCount = allAuctions.stream().filter(a -> "FINISHED".equalsIgnoreCase(a.getStatus()) || "PAID".equalsIgnoreCase(a.getStatus())).count();
        statFinished.setText(String.valueOf(finishedCount));

        int totalBids = allAuctions.stream().mapToInt(AuctionSummaryDTO::getBidCount).sum();
        statBids.setText(String.valueOf(totalBids));
    }

    /**
     * Đón nhận hành động lọc danh mục sản phẩm (Category Filter Action).
     * Hoàn tác phong cách đồ họa của các nút bấm và đồng bộ tham số lọc hệ thống theo danh mục được nhắm tới.
     *
     * @param event Sự kiện Action nhấp chuột gửi từ giao diện người dùng
     */
    @FXML
    void handleFilterCategory(ActionEvent event) {
        Button btn = (Button) event.getSource();
        resetCategoryStyles();
        btn.getStyleClass().setAll("filter-btn-active"); // Kích hoạt style CSS sáng màu cho phần tử chọn

        if (btn == filterElectronics) currentCategory = "ELECTRONICS";
        else if (btn == filterArt) currentCategory = "ART";
        else if (btn == filterVehicle) currentCategory = "VEHICLE";
        else currentCategory = "all";

        applyFilters();
    }

    /**
     * Đón nhận hành động lọc trạng thái vòng đời phiên đấu giá (Status Filter Action).
     * Cập nhật tham số trạng thái hoạt động mục tiêu và tái kích hoạt động cơ cấu trúc lưới.
     *
     * @param event Sự kiện Action nhấp chuột gửi từ giao diện người dùng
     */
    @FXML
    void handleFilterStatus(ActionEvent event) {
        Button btn = (Button) event.getSource();
        resetStatusStyles();
        btn.getStyleClass().setAll("filter-btn-active");

        if (btn == statusRunning) currentStatus = "RUNNING";
        else if (btn == statusOpen) currentStatus = "OPEN";
        else if (btn == statusFinished) currentStatus = "FINISHED";
        else currentStatus = "all";

        applyFilters();
    }

    /**
     * Hoàn tác các class định dạng phong cách CSS của nhóm nút bấm phân loại danh mục về trạng thái mặc định.
     */
    private void resetCategoryStyles() {
        filterAll.getStyleClass().setAll("filter-btn");
        filterElectronics.getStyleClass().setAll("filter-btn");
        filterArt.getStyleClass().setAll("filter-btn");
        filterVehicle.getStyleClass().setAll("filter-btn");
    }

    /**
     * Hoàn tác các class định dạng phong cách CSS của nhóm nút bấm lọc trạng thái về trạng thái mặc định.
     */
    private void resetStatusStyles() {
        statusAll.getStyleClass().setAll("filter-btn");
        statusRunning.getStyleClass().setAll("filter-btn");
        statusOpen.getStyleClass().setAll("filter-btn");
        statusFinished.getStyleClass().setAll("filter-btn");
    }

    /**
     * Động cơ xử lý bộ lọc đa tầng (Multi-tier Filtering Engine).
     * Áp dụng đồng thời liên hoàn 3 mệnh đề điều kiện (Danh mục, Vòng đời trạng thái, Chuỗi văn bản tìm kiếm)
     * để trích xuất tập phân mảnh dữ liệu đích từ RAM, sau đó chuyển giao cho hàm kết xuất đồ họa.
     */
    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase();

        List<AuctionSummaryDTO> filtered = allAuctions.stream()
                .filter(a -> "all".equals(currentCategory) || currentCategory.equalsIgnoreCase(a.getItemType()))
                .filter(a -> "all".equals(currentStatus) || currentStatus.equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getItemName().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        renderGrid(filtered);
    }

    /** Map cấu trúc lưu trữ duy trì trạng thái cache của các cấu trúc đồ họa thẻ sản phẩm (Flyweight Pattern). */
    private final java.util.Map<String, CardCache> cardCacheMap = new java.util.HashMap<>();

    /**
     * Kiến trúc lớp tĩnh (Static Inner Class) đóng gói cấu trúc cặp đối tượng
     * Node giao diện đồ họa và Controller quản lý liên đới phục vụ cơ chế tái sử dụng cache.
     */
    private static class CardCache {
        final Node node;
        final ProductCardController controller;

        CardCache(Node node, ProductCardController controller) {
            this.node = node;
            this.controller = controller;
        }
    }

    /**
     * Động cơ kết xuất cấu trúc lưới đồ họa (Grid Layout Rendering Engine).
     * Thực hiện làm sạch hoàn toàn vùng hiển thị cũ của FlowPane, duyệt qua danh sách thực thể đích,
     * tái sử dụng lại các Node đồ họa có sẵn từ cache hoặc tải mới qua FXMLLoader nếu chưa tồn tại,
     * nạp thuộc tính DTO mới và đính kèm phần tử vào lưới hiển thị view.
     *
     * @param auctions Danh sách thực thể tóm tắt các phiên đấu giá đã qua bộ lọc xử lý
     */
    private void renderGrid(List<AuctionSummaryDTO> auctions) {
        auctionGrid.getChildren().clear(); // Làm sạch toàn bộ các phần tử đồ họa con cũ trong lưới FlowPane
        for (AuctionSummaryDTO auction : auctions) {
            try {
                CardCache cache = cardCacheMap.get(auction.getAuctionId());
                if (cache == null) {
                    // Tải mới file cấu hình giao diện thẻ sản phẩm nếu chưa được lưu vết trong cache
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductCard.fxml"));
                    Node node = loader.load();
                    ProductCardController controller = loader.getController();
                    cache = new CardCache(node, controller);
                    cardCacheMap.put(auction.getAuctionId(), cache); // Đẩy vào cấu trúc lưu trữ cache
                }
                cache.controller.setData(auction); // Đồng bộ nạp dữ liệu DTO mới vào Controller của thẻ sản phẩm
                auctionGrid.getChildren().add(cache.node); // Đính kèm Node đồ họa vào cấu trúc hiển thị FlowPane
            } catch (IOException e) {
                LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình nạp cây tài nguyên thẻ sản phẩm đồ họa fxml", e);
            }
        }
    }
}