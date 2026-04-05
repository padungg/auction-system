package com.auction.client; // Kiểm tra lại dòng này cho khớp với package của cậu

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Lệnh máy: Tìm file giao diện trong thư mục resources
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("/hello-view.fxml"));

        // Tạo cửa sổ kích thước 600x400
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);

        stage.setTitle("Hệ thống Đấu giá 2026");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}