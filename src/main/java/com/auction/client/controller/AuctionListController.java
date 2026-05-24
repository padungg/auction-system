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
 * Controller quản lý danh sách đấu giá công khai.
 */
public class AuctionListController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionListController.class);

    // STATS & SEARCH FXML
    @FXML private Label statTotal, statActive, statBids, statFinished;
    @FXML private TextField txtSearch;
    @FXML private FlowPane auctionGrid;

    // FILTER CONTROLS FXML
    @FXML private Button filterAll, filterElectronics, filterArt, filterVehicle;
    @FXML private Button statusAll, statusRunning, statusOpen, statusFinished;

    private List<AuctionSummaryDTO> allAuctions = new ArrayList<>();
    private String currentCategory = "all";
    private String currentStatus = "all";
    private final java.util.Map<String, CardCache> cardCacheMap = new java.util.HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadAuctions();

        // Lắng nghe thay đổi tìm kiếm thời gian thực
        txtSearch.textProperty().addListener((_, _, _) -> applyFilters());
    }

    /**
     * Tải danh sách phiên đấu giá từ server.
     */
    private void loadAuctions() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response res = ClientSocketManager.getInstance().sendRequest(req);

                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    allAuctions = res.getPayloadAs(new TypeToken<>(){});
                } else {
                    allAuctions = new ArrayList<>();
                    LOGGER.warn("Không tải được dữ liệu từ DB, hiển thị danh sách rỗng.");
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố kết nối lỗi mạng khi thực thi yêu cầu GET_ALL_AUCTIONS", e);
                allAuctions = new ArrayList<>();
            }
            Platform.runLater(() -> {
                cardCacheMap.clear();
                updateStats();
                applyFilters();
            });
        });
    }

    /**
     * Cập nhật nhãn thống kê hệ thống.
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
     * Lọc danh mục sản phẩm.
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
     * Lọc trạng thái phiên đấu giá.
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

    private void resetCategoryStyles() {
        filterAll.getStyleClass().setAll("filter-btn");
        filterElectronics.getStyleClass().setAll("filter-btn");
        filterArt.getStyleClass().setAll("filter-btn");
        filterVehicle.getStyleClass().setAll("filter-btn");
    }

    private void resetStatusStyles() {
        statusAll.getStyleClass().setAll("filter-btn");
        statusRunning.getStyleClass().setAll("filter-btn");
        statusOpen.getStyleClass().setAll("filter-btn");
        statusFinished.getStyleClass().setAll("filter-btn");
    }

    /**
     * Áp dụng bộ lọc đa tầng lên danh sách.
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
     * Record lưu giữ cache đồ họa thẻ sản phẩm (Flyweight Pattern).
     */
    private record CardCache(Node node, ProductCardController controller) {}

    /**
     * Kết xuất cấu trúc lưới đồ họa thẻ sản phẩm.
     */
    private void renderGrid(List<AuctionSummaryDTO> auctions) {
        auctionGrid.getChildren().clear();
        for (AuctionSummaryDTO auction : auctions) {
            try {
                CardCache cache = cardCacheMap.get(auction.getAuctionId());
                if (cache == null) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductCard.fxml"));
                    Node node = loader.load();
                    ProductCardController controller = loader.getController();
                    cache = new CardCache(node, controller);
                    cardCacheMap.put(auction.getAuctionId(), cache);
                }
                cache.controller().setData(auction);
                auctionGrid.getChildren().add(cache.node());
            } catch (IOException e) {
                LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình nạp cây tài nguyên thẻ sản phẩm đồ họa fxml", e);
            }
        }
    }
}