package com.auction.server.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class HelloController {

    // 1. Khai báo các linh kiện trên màn hình
    @FXML
    private TextField usernameButton;

    @FXML
    private PasswordField passwordButton;

    @FXML
    private Label textLogin;

    // 2. Hàm này sẽ chạy khi cậu nhấn nút "Đăng nhập"
    @FXML
    public void handleLogin() {
        String user = usernameButton.getText();
        String pass = passwordButton.getText();

        // Cậu nhấc máy gọi cho Server ở cổng 1234
        try (Socket socket = new Socket("localhost", 1234)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Gửi tài khoản và mật khẩu sang Server qua ống Socket
            // Ví dụ gửi: "admin,123"
            out.writeUTF(user + "," + pass);
            out.flush();

            // Đợi Server đọc file database.txt rồi trả lời Đúng/Sai
            boolean isOk = in.readBoolean();

            if (isOk) {
                textLogin.setText("ĐĂNG NHẬP THÀNH CÔNG!");
            } else {
                textLogin.setText("SAI TÀI KHOẢN HOẶC MẬT KHẨU!");
            }

        } catch (Exception e) {
            // Nếu chưa bật file ServerApp.java thì sẽ nhảy vào đây
            textLogin.setText("LỖI: SERVER CHƯA BẬT!");
        }
    }

    // Các hàm này để tránh lỗi khi FXML tìm kiếm Action
    @FXML public void handleUsername() {}
    @FXML public void handlePassword() {}
}