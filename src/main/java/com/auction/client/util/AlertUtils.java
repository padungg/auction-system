package com.auction.client.util;

import javafx.scene.control.Alert;

/**
 * Lớp tiện ích quản lý hiển thị các hộp thoại thông báo tập trung.
 */
public class AlertUtils {

    public static void showWarning(String title, String msg) {
        showAlert(Alert.AlertType.WARNING, title, msg);
    }

    public static void showInfo(String title, String msg) {
        showAlert(Alert.AlertType.INFORMATION, title, msg);
    }

    public static void showError(String title, String msg) {
        showAlert(Alert.AlertType.ERROR, title, msg);
    }

    public static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}