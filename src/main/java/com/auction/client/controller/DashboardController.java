package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML
    private GridPane productGrid;

    @FXML
    public void initialize() {
        refreshData();
    }

    public void refreshData() {
        List<AuctionSummaryDTO> auctions = getAuctionsFromServer();
        displayProducts(auctions);
    }

    private void displayProducts(List<AuctionSummaryDTO> auctions) {
        if (productGrid != null) {
            productGrid.getChildren().clear();
            int column = 0;
            int row = 0;
            int maxColumns = 3;
            for (AuctionSummaryDTO auction : auctions) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/productcard.fxml"));
                    VBox card = loader.load();

                    ProductcardController controller = loader.getController();
                    controller.setData(auction);

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


    private List<AuctionSummaryDTO> getAuctionsFromServer() {
        try {
            ClientSocketManager manager = ClientSocketManager.getInstance();

            if (!manager.isConnected()) {
                System.err.println("[Dashboard] Chưa kết nối đến Server.");
                return getSampleData();
            }

            Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
            Response response = manager.sendRequest(request);

            if (response.getStatus() == ResponseStatus.SUCCESS) {
                Gson gson = manager.getGson();
                String payloadJson = gson.toJson(response.getPayload());
                List<AuctionSummaryDTO> list = gson.fromJson(payloadJson,
                        new TypeToken<List<AuctionSummaryDTO>>() {}.getType());
                return list != null ? list : new ArrayList<>();
            } else {
                System.err.println("[Dashboard] Server trả lỗi: " + response.getMessage());
                return getSampleData();
            }

        } catch (IOException e) {
            System.err.println("[Dashboard] Lỗi kết nối: " + e.getMessage());
            return getSampleData();
        }
    }

    private List<AuctionSummaryDTO> getSampleData() {
        List<AuctionSummaryDTO> list = new ArrayList<>();
        list.add(new AuctionSummaryDTO("A001", "iPhone 17 Pro", 35000000, "OPENING"));
        list.add(new AuctionSummaryDTO("A002", "MacBook Air M4", 28000000, "OPENING"));
        list.add(new AuctionSummaryDTO("A003", "Tranh sơn dầu", 5000000, "PENDING"));
        list.add(new AuctionSummaryDTO("A004", "Honda SH 150i", 75000000, "OPENING"));
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
        refreshData();
    }
}