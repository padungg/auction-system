package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.network.Request;
import com.auction.client.network.Response;
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
    private int productId = 1; // ID sản phẩm đang xem (sau này truyền từ Dashboard)

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
        registerRealTimeListener();
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
                Request request = new Request("PLACE_BID")
                        .put("productId", productId)
                        .put("amount", newPrice);

                Response response = client.sendRequest(request);

                if (response != null && response.isSuccess()) {
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
     *
     * Server sẽ gửi:
     * {"status":"OK","action":"NOTIFY_NEW_BID","data":{"productId":1,"newPrice":17000000,"bidder":"user2"}}
     *
     * Callback này được gọi trên UI thread (safe để cập nhật UI).
     */
    private void registerRealTimeListener() {
        ClientSocketManager client = ClientSocketManager.getInstance();

        if (client.isConnected()) {
            client.addNotificationListener(response -> {
                if ("NOTIFY_NEW_BID".equals(response.getAction()) && response.getData() != null) {
                    // Kiểm tra đúng sản phẩm đang xem
                    Object pidObj = response.getData().get("productId");
                    int pid = pidObj instanceof Number ? ((Number) pidObj).intValue() : -1;

                    if (pid == productId) {
                        long newPrice = ((Number) response.getData().get("newPrice")).longValue();
                        String bidder = (String) response.getData().get("bidder");

                        // Cập nhật UI (đã ở UI thread nhờ Platform.runLater trong ClientSocketManager)
                        currentPrice = newPrice;
                        currentPriceLabel.setText(currentPrice + " VNĐ");
                        list.add(0, new Bid("now", bidder, newPrice + ""));

                        System.out.println("[PaymentController] Real-time: " + bidder + " đặt giá " + newPrice);
                    }
                }
            });
        }
    }

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