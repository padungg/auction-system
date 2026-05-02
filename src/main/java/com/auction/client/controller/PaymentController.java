package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.BidRequestDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
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
    private String auctionId = "A001"; // ID phiên đấu giá đang xem (sau này truyền từ Dashboard)

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
        // registerRealTimeListener(); // Tạm tắt — ClientSocketManager chưa hỗ trợ addNotificationListener
    }

    /**
     * Gửi request đặt giá đến Server.
     *
     * Gửi:  Request(PLACE_BID, BidRequestDTO{auctionId, bidAmount})
     * Nhận:  Response(SUCCESS, "Đặt giá thành công", payload)
     *        hoặc Response(ERROR/BAD_REQUEST, "Giá phải cao hơn hiện tại", null)
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
                // Gửi request PLACE_BID đến Server — dùng BidRequestDTO + RequestType enum
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
     * (Tạm thời comment vì ClientSocketManager chưa hỗ trợ addNotificationListener).
     *
     * TODO: Bật lại khi ClientSocketManager được bổ sung addNotificationListener()
     *       và Server hoàn thiện phần push notification qua Observer pattern.
     */
    /*
    private void registerRealTimeListener() {
        ClientSocketManager client = ClientSocketManager.getInstance();

        if (client.isConnected()) {
            client.addNotificationListener(response -> {
                // Xử lý push notification từ server khi có bid mới
                // Cần cập nhật lại khi ClientSocketManager hỗ trợ
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