package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ManagementController {

    @FXML private TableView<AuctionSummaryDTO> tableProducts;
    @FXML private TableColumn<AuctionSummaryDTO, String> colId;
    @FXML private TableColumn<AuctionSummaryDTO, String> colName;
    @FXML private TableColumn<AuctionSummaryDTO, String> colCategory;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colPrice;
    @FXML private TableColumn<AuctionSummaryDTO, String> colStatus;
    @FXML private TableColumn<AuctionSummaryDTO, String> colTime;

    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField txtPrice;
    @FXML private TextField txtStartTime;
    @FXML private TextField txtEndTime;
    @FXML private TextField txtImagePath;
    @FXML private TextArea txtDescription;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;

    private ObservableList<AuctionSummaryDTO> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableProducts.setItems(productList);

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                txtName.setText(newItem.getItemName());
                txtPrice.setText(String.valueOf(newItem.getCurrentPrice()));
            }
        });

        cbCategory.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));

        loadAuctions();
    }

    private void loadAuctions() {
        new Thread(() -> {
            try {
                ClientSocketManager client = ClientSocketManager.getInstance();
                if (client.isConnected()) {
                    Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
                    Response response = client.sendRequest(request);
                    if (response.getStatus() == ResponseStatus.SUCCESS) {
                        AuctionSummaryDTO[] arr = client.getGson().fromJson(
                                client.getGson().toJson(response.getPayload()),
                                AuctionSummaryDTO[].class);
                        Platform.runLater(() -> {
                            productList.clear();
                            if (arr != null) productList.addAll(arr);
                        });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleAdd() {
        if (txtName.getText().isEmpty() || txtPrice.getText().isEmpty() || cbCategory.getValue() == null) {
            showAlert("Lỗi", "Vui lòng nhập đủ thông tin (Tên, Giá, Loại)!");
            return;
        }

        CreateAuctionDTO dto = new CreateAuctionDTO();
        dto.setName(txtName.getText());
        dto.setStartingPrice(Double.parseDouble(txtPrice.getText()));
        dto.setItemType(cbCategory.getValue());
        dto.setCondition("NEW");
        dto.setDescription(txtDescription.getText());
        dto.setDurationDays(3);

        new Thread(() -> {
            try {
                ClientSocketManager client = ClientSocketManager.getInstance();
                if (client.isConnected()) {
                    Request request = new Request(RequestType.CREATE_AUCTION, dto);
                    Response response = client.sendRequest(request);

                    Platform.runLater(() -> {
                        if (response.getStatus() == ResponseStatus.SUCCESS) {
                            showAlert("Thành công", "Tạo phiên đấu giá thành công!");
                            clearForm();
                            loadAuctions();
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleUpdate() {
        showAlert("Thông báo", "Chức năng cập nhật đang được phát triển!");
    }

    @FXML
    private void handleDelete() {
        showAlert("Thông báo", "Chức năng xóa đang được phát triển!");
    }

    private void clearForm() {
        txtName.clear();
        txtPrice.clear();
        txtStartTime.clear();
        txtEndTime.clear();
        txtImagePath.clear();
        txtDescription.clear();
        cbCategory.setValue(null);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}