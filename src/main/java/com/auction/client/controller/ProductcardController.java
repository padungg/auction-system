package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import com.auction.model.dto.AuctionSummaryDTO;

public class ProductcardController {

    @FXML
    private ImageView imgProduct;

    @FXML
    private Label lblProductName;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblStartPrice;

    @FXML
    private Label lblCurrentPrice;

    @FXML
    private Label lblTimeLeft;

    @FXML
    private Button btnJoin;

    /**
     * Nhận dữ liệu từ AuctionSummaryDTO (DTO của server) và hiển thị lên card.
     */
    public void setData(AuctionSummaryDTO auction) {
        lblProductName.setText(auction.getItemName());
        lblStatus.setText(mapStatus(auction.getStatus()));
        lblStartPrice.setText("ID: " + auction.getAuctionId());
        lblCurrentPrice.setText(String.format("%.0f đ", auction.getCurrentPrice()));
        lblTimeLeft.setText("Trạng thái: " + auction.getStatus());
    }

    /**
     * Chuyển đổi status code từ server sang hiển thị thân thiện.
     */
    private String mapStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "OPENING": return "🟢 Đang diễn ra";
            case "PENDING": return "🟡 Sắp diễn ra";
            case "CLOSED":  return "🔴 Đã kết thúc";
            case "CANCELLED": return "⚪ Đã hủy";
            default: return status;
        }
    }

    @FXML
    private void handleJoin() {
        System.out.println("Click vào: " + lblProductName.getText());
    }
}