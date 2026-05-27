package com.auction.client.util;

import javafx.scene.control.Alert;

/**
 * Tiện ích hiển thị các hộp thoại thông báo (Alert) trong JavaFX.
 */
public class AlertUtils {

    /**
     * Hiển thị hộp thoại cảnh báo (Warning).
     */
    public static void showWarning(String title, String msg) {
        showAlert(Alert.AlertType.WARNING, title, msg);
    }

    /**
     * Hiển thị hộp thoại thông tin (Info).
     */
    public static void showInfo(String title, String msg) {
        showAlert(Alert.AlertType.INFORMATION, title, msg);
    }

    /**
     * Hiển thị hộp thoại lỗi (Error).
     */
    public static void showError(String title, String msg) {
        showAlert(Alert.AlertType.ERROR, title, msg);
    }

    /**
     * Khởi tạo và hiển thị hộp thoại Alert.
     */
    public static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}