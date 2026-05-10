package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import com.auction.model.dto.AuctionSummaryDTO;
import java.io.IOException;

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

    private AuctionSummaryDTO auctionData;

    public void setData(AuctionSummaryDTO auction) {
        this.auctionData = auction;
        lblProductName.setText(auction.getItemName());
        lblStatus.setText(mapStatus(auction.getStatus()));
        lblStartPrice.setText("ID: " + auction.getAuctionId());
        lblCurrentPrice.setText(String.format("%.0f đ", auction.getCurrentPrice()));
        lblTimeLeft.setText("Trạng thái: " + auction.getStatus());
    }

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
    private void handleJoin(ActionEvent event) {
        if (auctionData == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/buy.fxml"));
            Parent root = loader.load();

            // Lấy controller của màn hình Payment và truyền dữ liệu
            PaymentController controller = loader.getController();
            controller.initData(auctionData);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Chi tiết phiên đấu giá");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}