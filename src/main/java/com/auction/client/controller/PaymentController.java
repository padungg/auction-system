package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.dto.BidRequestDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PaymentController {

    @FXML
    private TextField bidInput;

    @FXML
    private Label currentPriceLabel;

    @FXML
    private TableView<Bid> bidTable;

    @FXML
    private TableColumn<Bid, String> timeColumn;

    @FXML
    private TableColumn<Bid, String> userColumn;

    @FXML
    private TableColumn<Bid, String> priceColumn;

    private long currentPrice = 15500000;
    private String auctionId = "A001"; // ID sản phẩm đang xem (sau này truyền từ Dashboard)

    private ObservableList<Bid> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Gán dữ liệu cho bảng
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().time));
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().user));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().price));

        bidTable.setItems(list);

        // Set giá ban đầu
        currentPriceLabel.setText(currentPrice + " VNĐ");

        // Đăng ký nhận push notification real-time (khi có người đặt giá mới)
        // registerRealTimeListener(); // Tạm thời comment vì mô hình hiện tại không dùng push kiểu này nữa
    }

    /**
     * Gửi request đặt giá đến Server.
     *
     * Gửi:  {"action":"PLACE_BID","data":{"productId":1,"amount":16000000}}
     * Nhận:  {"status":"OK","action":"PLACE_BID","message":"Đặt giá thành công"}
     *        hoặc {"status":"ERROR","action":"PLACE_BID","message":"Giá phải cao hơn hiện tại"}
     */
    @FXML
    private void handleBid() {
        String input = bidInput.getText();

        if (input.isEmpty()) return;

        try {
            long newPrice = Long.parseLong(input);

            if (newPrice <= currentPrice) return;

            ClientSocketManager client = ClientSocketManager.getInstance();

            if (client.isConnected()) {
                // Gửi request PLACE_BID đến Server
                BidRequestDTO dto = new BidRequestDTO(auctionId, (double) newPrice);
                Request request = new Request(RequestType.PLACE_BID, dto);

                Response response = client.sendRequest(request);

                if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                    // Server xác nhận → cập nhật UI
                    currentPrice = newPrice;
                    currentPriceLabel.setText(currentPrice + " VNĐ");
                    list.add(0, new Bid("now", "Bạn", newPrice + ""));
                    bidInput.clear();
                    System.out.println("[PaymentController] Đặt giá thành công: " + newPrice);
                } else {
                    System.err.println("[PaymentController] Đặt giá thất bại: "
                            + (response != null ? response.getMessage() : "timeout"));
                }
            } else {
                // Fallback: chưa có server → cập nhật local (như cũ)
                currentPrice = newPrice;
                currentPriceLabel.setText(currentPrice + " VNĐ");
                list.add(0, new Bid("now", "Bạn", newPrice + ""));
                bidInput.clear();
            }

        } catch (Exception e) {
            // Nhập sai thì bỏ qua
        }
    }

    /**
     * Đăng ký nhận push notification từ Server khi có người đặt giá mới.
     * (Tạm thời comment vì ClientSocketManager mới không hỗ trợ addNotificationListener).
     */
    /*
    private void registerRealTimeListener() {
        ClientSocketManager client = ClientSocketManager.getInstance();

        if (client.isConnected()) {
            client.addNotificationListener(response -> {
                // ... logic cũ ...
            });
        }
    }
    */

    // Class lưu dữ liệu bảng
    public static class Bid {
        String time;
        String user;
        String price;

        public Bid(String time, String user, String price) {
            this.time = time;
            this.user = user;
            this.price = price;
        }
    }
}