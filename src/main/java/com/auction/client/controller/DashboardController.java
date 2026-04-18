package com.auction.client.controller;

import com.auction.client.model.Product;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class DashboardController {

    @FXML
    private GridPane productGrid;

    @FXML
    public void initialize() {
        refreshData();
    }

    public void refreshData() {

        List<Product> products = getSampleData();
        displayProducts(products);
    }

    private void displayProducts(List<Product> products) {
        if (productGrid != null) {
            productGrid.getChildren().clear();
            int column = 0;
            int row = 0;
            int maxColumns = 3;
            for (Product product : products) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/productcard.fxml"));
                    VBox card = loader.load();

                    ProductcardController controller = loader.getController();
                    controller.setData(product);

                    productGrid.add(card, column, row);
                    column++;
                    if (column >= maxColumns) {
                        column = 0;
                        row++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<Product> getSampleData() {
        List<Product> list = new java.util.ArrayList<>();

        list.add(new Product(1, "iPhone 17 Pro", "Điện tử", 35000000, "🟢 Đang diễn ra", "20:00 20/04"));
        list.add(new Product(2, "MacBook Air M4", "Điện tử", 28000000, "🟢 Đang diễn ra", "22:00 21/04"));
        list.add(new Product(3, "Tranh sơn dầu", "Nghệ thuật", 5000000, "🟡 Sắp diễn ra", "10:00 25/04"));
        list.add(new Product(4, "Honda SH 150i", "Phương tiện", 75000000, "🟢 Đang diễn ra", "18:00 22/04"));
        return list;
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/buy.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Chi tiết sản phẩm");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleManage(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/manage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Quản lý sản phẩm");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Danh sách đấu giá");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}