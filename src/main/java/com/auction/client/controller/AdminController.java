package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminController {

    @FXML
    private TextField txtSearchUser;

    @FXML
    private TableView<?> tableUsers;

    @FXML
    private TableColumn<?, ?> colUserId;

    @FXML
    private TableColumn<?, ?> colUsername;

    @FXML
    private TableColumn<?, ?> colRole;

    @FXML
    private TableColumn<?, ?> colUserStatus;

    @FXML
    public void initialize() {
    }

    @FXML
    private void handleLockUser() {
        Object selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Lock user: " + selected);
        }
    }

    @FXML
    private void handleUnlockUser() {
        Object selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Unlock user: " + selected);
        }
    }

    @FXML
    private void handleGrantSeller() {
        Object selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Grant seller: " + selected);
        }
    }
}