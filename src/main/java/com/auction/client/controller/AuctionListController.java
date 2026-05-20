package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
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
 * Bộ điều khiển phân hệ Danh sách đấu giá (Auction List Controller).
 * Đảm nhiệm vai trò hiển thị danh sách các phiên đấu giá công khai, thống kê chỉ số tổng quan hệ thống,
 * thực hiện bộ lọc đa tiêu chí (danh mục sản phẩm, trạng thái phiên) và tìm kiếm động theo từ khóa trong luồng JavaFX.
 */
public class AuctionListController implements Initializable {

    /**
     * Khởi tạo thành phần Logger theo tiêu chuẩn SLF4J nhằm giám sát tiến trình nạp tài nguyên giao diện,
     * theo dõi gói tin kết nối mạng và lưu vết lỗi I/O bất đồng bộ trong chu kỳ xử lý Grid View.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionListController.class);

    private final Gson GSON = GsonProvider.getInstance();

    @FXML private Label statTotal, statActive, statBids, statFinished;
    @FXML private TextField txtSearch;
    @FXML private FlowPane auctionGrid;

    @FXML private Button filterAll, filterElectronics, filterArt, filterVehicle;
    @FXML private Button statusAll, statusRunning, statusOpen, statusFinished;

    private List<AuctionSummaryDTO> allAuctions = new ArrayList<>();
    private String currentCategory = "all";
    private String currentStatus = "all";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadAuctions();

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    /**
     * Khởi chạy Worker Thread đồng bộ danh sách toàn bộ các phiên đấu giá hiện hành từ Server,
     * tự động chuyển tiếp dữ liệu về JavaFX UI Thread để cập nhật chỉ số đo lường định lượng.
     */
    private void loadAuctions() {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    allAuctions = GSON.fromJson(GSON.toJson(res.getPayload()), new TypeToken<List<AuctionSummaryDTO>>(){}.getType());
                } else {
                    allAuctions = new ArrayList<>();
                    LOGGER.warn("Không tải được dữ liệu từ DB, hiển thị danh sách rỗng.");
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố kết nối lỗi mạng khi thực thi yêu cầu GET_ALL_AUCTIONS", e);
                allAuctions = new ArrayList<>();
            }
            Platform.runLater(() -> {
                updateStats();
                applyFilters();
            });
        }).start();
    }

    /**
     * Phân tích, tổng hợp luồng dữ liệu RAM và kết xuất các chỉ số đo lường trạng thái phiên đấu giá lên bảng điều khiển.
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
     * Tiếp nhận sự kiện và cập nhật trạng thái phân loại danh mục sản phẩm (Thiết bị điện tử, Nghệ thuật, Phương tiện...).
     */
    @FXML
    void handleFilterCategory(ActionEvent event) {
        Button btn = (Button) event.getSource();
        resetCategoryStyles();
        btn.getStyleClass().setAll("filter-btn-active");

        if (btn == filterElectronics) currentCategory = "ELECTRONICS";
        else if (btn == filterArt) currentCategory = "ART";
        else if (btn == filterVehicle) currentCategory = "VEHICLE";
        else currentCategory = "all";

        applyFilters();
    }

    /**
     * Tiếp nhận sự kiện và cập nhật bộ lọc theo vòng đời trạng thái của phiên (Đang diễn ra, Chờ kích hoạt, Đã kết thúc).
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
     * Hoàn tác lớp CSS định dạng thanh công cụ phân loại danh mục sản phẩm về trạng thái mặc định.
     */
    private void resetCategoryStyles() {
        filterAll.getStyleClass().setAll("filter-btn");
        filterElectronics.getStyleClass().setAll("filter-btn");
        filterArt.getStyleClass().setAll("filter-btn");
        filterVehicle.getStyleClass().setAll("filter-btn");
    }

    /**
     * Hoàn tác lớp CSS định dạng thanh công cụ phân loại trạng thái vòng đời về trạng thái mặc định.
     */
    private void resetStatusStyles() {
        statusAll.getStyleClass().setAll("filter-btn");
        statusRunning.getStyleClass().setAll("filter-btn");
        statusOpen.getStyleClass().setAll("filter-btn");
        statusFinished.getStyleClass().setAll("filter-btn");
    }

    /**
     * Thực hiện bóc tách, sàng lọc danh sách nguồn dựa trên các tham số danh mục, trạng thái và văn bản tìm kiếm động.
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

    /**
     * Dọn dẹp không gian hiển thị cũ, nạp động tệp tin cấu hình và phân bổ các Thẻ sản phẩm (Product Card) lên lưới bố cục FlowPane.
     */
    private void renderGrid(List<AuctionSummaryDTO> auctions) {
        auctionGrid.getChildren().clear();
        for (AuctionSummaryDTO auction : auctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/productcard.fxml"));
                Node card = loader.load();
                ProductcardController controller = loader.getController();
                controller.setData(auction);
                auctionGrid.getChildren().add(card);
            } catch (IOException e) {
                LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình nạp cây tài nguyên thẻ sản phẩm đồ họa fxml", e);
            }
        }
    }
}