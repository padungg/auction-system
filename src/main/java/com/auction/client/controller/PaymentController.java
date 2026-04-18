package com.auction.client.controller;

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

    private ObservableList<Bid> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // gán dữ liệu cho bảng
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().time));
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().user));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().price));

        bidTable.setItems(list);

        // set giá ban đầu
        currentPriceLabel.setText(currentPrice + " VNĐ");
    }

    @FXML
    private void handleBid() {
        String input = bidInput.getText();

        if (input.isEmpty()) return;

        try {
            long newPrice = Long.parseLong(input);

            if (newPrice <= currentPrice) return;

            // cập nhật giá
            currentPrice = newPrice;
            currentPriceLabel.setText(currentPrice + " VNĐ");

            // thêm vào bảng
            list.add(0, new Bid("now", "Bạn", newPrice + ""));

            bidInput.clear();

        } catch (Exception e) {
            // nhập sai thì bỏ qua
        }
    }

    // class lưu dữ liệu bảng
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