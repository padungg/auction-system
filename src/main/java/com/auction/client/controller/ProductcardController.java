package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import com.auction.client.model.Product;

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

    public void setData(Product product) {
        lblProductName.setText(product.getName());
        lblStatus.setText(product.getStatus());
        lblStartPrice.setText("Danh mục: " + product.getCategory());
        lblCurrentPrice.setText(String.format("%.0f đ", product.getPrice()));
        lblTimeLeft.setText("Kết thúc: " + product.getEndTime());
    }

    @FXML
    private void handleJoin() {
        System.out.println("Click vào: " + lblProductName.getText());
    }
}