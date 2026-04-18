package com.auction.client.controller;
import com.auction.client.model.Product;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ManagementController {

    @FXML
    private TableView<Product> tableProducts;

    @FXML
    private TableColumn<Product, Integer> colId;

    @FXML
    private TableColumn<Product, String> colName;

    @FXML
    private TableColumn<Product, String> colCategory;

    @FXML
    private TableColumn<Product, Double> colPrice;

    @FXML
    private TableColumn<Product, String> colStatus;

    @FXML
    private TableColumn<Product, String> colTime;

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

    private ObservableList<Product> productList = FXCollections.observableArrayList();

    private int currentId = 1;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        tableProducts.setItems(productList);

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                txtName.setText(newItem.getName());
                cbCategory.setValue(newItem.getCategory());
                txtPrice.setText(String.valueOf(newItem.getPrice()));
                txtEndTime.setText(newItem.getEndTime());
            }
        });

        cbCategory.setItems(FXCollections.observableArrayList("Điện tử", "Thời trang", "Khác"));
    }

    @FXML
    private void handleAdd() {
        Product p = new Product(
                currentId++,
                txtName.getText(),
                cbCategory.getValue(),
                Double.parseDouble(txtPrice.getText()),
                "Đang diễn ra",
                txtEndTime.getText()
        );

        productList.add(p);
        clearForm();
    }

    @FXML
    private void handleUpdate() {
        Product selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        selected.setName(txtName.getText());
        selected.setCategory(cbCategory.getValue());
        selected.setPrice(Double.parseDouble(txtPrice.getText()));
        selected.setEndTime(txtEndTime.getText());

        tableProducts.refresh();
        clearForm();
    }

    @FXML
    private void handleDelete() {
        Product selected = tableProducts.getSelectionModel().getSelectedItem();
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