package com.auction.client.util;

import javafx.scene.control.Alert;

/**
 * Lớp tiện ích quản lý hiển thị các hộp thoại thông báo tập trung.
 */
public class AlertUtils {

    /**
     * Hiển thị hộp thoại cảnh báo (Warning).
     *
     * @param title Tiêu đề hộp thoại
     * @param msg   Nội dung cảnh báo
     */
    public static void showWarning(String title, String msg) {
        showAlert(Alert.AlertType.WARNING, title, msg);
    }

    /**
     * Hiển thị hộp thoại thông tin (Information).
     *
     * @param title Tiêu đề hộp thoại
     * @param msg   Nội dung thông tin
     */
    public static void showInfo(String title, String msg) {
        showAlert(Alert.AlertType.INFORMATION, title, msg);
    }

    /**
     * Hiển thị hộp thoại thông báo lỗi (Error).
     *
     * @param title Tiêu đề hộp thoại
     * @param msg   Nội dung thông báo lỗi
     */
    public static void showError(String title, String msg) {
        showAlert(Alert.AlertType.ERROR, title, msg);
    }

    /**
     * Khởi tạo và hiển thị hộp thoại cảnh báo/thông báo theo cấu hình được chỉ định.
     *
     * @param type  Loại thông báo (Alert Type)
     * @param title Tiêu đề hộp thoại
     * @param msg   Nội dung chi tiết
     */
    public static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}