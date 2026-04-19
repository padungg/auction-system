package com.auction.client.controller;

import com.auction.model.dto.AuctionSummaryDTO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller quản lý sản phẩm.
 * Hiện tại vẫn dùng logic local (chưa gọi Server CREATE_AUCTION API).
 * Đã chuyển sang dùng AuctionSummaryDTO thay vì Product cũ.
 */
public class ManagementController {

    @FXML
    private TableView<AuctionSummaryDTO> tableProducts;

    @FXML
    private TableColumn<AuctionSummaryDTO, String> colId;

    @FXML
    private TableColumn<AuctionSummaryDTO, String> colName;

    @FXML
    private TableColumn<AuctionSummaryDTO, String> colCategory;

    @FXML
    private TableColumn<AuctionSummaryDTO, Double> colPrice;

    @FXML
    private TableColumn<AuctionSummaryDTO, String> colStatus;

    @FXML
    private TableColumn<AuctionSummaryDTO, String> colTime;

    @FXML
    private TextField txtName;

    @FXML
    private ComboBox<String> cbCategory;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtStartTime;

    @FXML
    private TextField txtEndTime;

    @FXML
    private TextField txtImagePath;

    @FXML
    private TextArea txtDescription;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnUpdate;

    @FXML
    private Button btnDelete;

    private ObservableList<AuctionSummaryDTO> productList = FXCollections.observableArrayList();

    private int currentId = 1;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        // colTime sẽ dùng khi có AuctionDetailDTO, tạm ẩn
        if (colTime != null) {
            colTime.setCellValueFactory(new PropertyValueFactory<>("status"));
        }

        tableProducts.setItems(productList);

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                txtName.setText(newItem.getItemName());
                cbCategory.setValue(newItem.getStatus());
                txtPrice.setText(String.valueOf(newItem.getCurrentPrice()));
            }
        });

        cbCategory.setItems(FXCollections.observableArrayList("Điện tử", "Thời trang", "Khác"));
    }

    @FXML
    private void handleAdd() {
        AuctionSummaryDTO item = new AuctionSummaryDTO(
                "A" + String.format("%03d", currentId++),
                txtName.getText(),
                Double.parseDouble(txtPrice.getText()),
                "PENDING"
        );

        productList.add(item);
        clearForm();
    }

    @FXML
    private void handleUpdate() {
        AuctionSummaryDTO selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        selected.setItemName(txtName.getText());
        selected.setCurrentPrice(Double.parseDouble(txtPrice.getText()));
        selected.setStatus(cbCategory.getValue());

        tableProducts.refresh();
        clearForm();
    }

    @FXML
    private void handleDelete() {
        AuctionSummaryDTO selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        productList.remove(selected);
        clearForm();
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
}