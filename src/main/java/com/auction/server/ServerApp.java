package com.auction.server;

import com.auction.server.database.UserDAOImpl;
import java.io.*;
import java.net.*;

public class ServerApp {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            // Thông báo khi bật máy chủ
            System.out.println(">>> [Hệ thống]: Kết nối Database thành công!");
            System.out.println(">>> [Hệ thống]: SERVER ĐANG CHẠY... ĐANG ĐỢI KẾT NỐI TẠI CỔNG 1234...");

            while (true) {
                Socket socket = serverSocket.accept();

                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                String request = in.readUTF();
                String[] parts = request.split(",");
                String user = parts[0];
                String pass = parts[1];

                // BẮT ĐẦU THÊM CÁC DÒNG THÔNG BÁO Ở ĐÂY:
                System.out.println("--------------------------------------------");
                System.out.println(">>> [Hệ thống]: Có thiết bị yêu cầu đăng nhập...");

                UserDAOImpl dao = new UserDAOImpl();
                boolean isOk = dao.checkLogin(user, pass);

                if (isOk) {
                    // Nếu đúng tài khoản trong file txt
                    System.out.println(">>> [Hệ thống]: Tài khoản '" + user + "' hợp lệ.");
                    System.out.println(">>> [Hệ thống]: Đăng nhập thành công!");
                    System.out.println(">>> Chào mừng Admin: " + user);
                } else {
                    // Nếu sai
                    System.out.println(">>> [Hệ thống]: Đăng nhập thất bại!");
                    System.out.println(">>> Cảnh báo: Sai tài khoản hoặc mật khẩu cho user: " + user);
                }
                System.out.println("--------------------------------------------");

                out.writeBoolean(isOk);
                out.flush();
                socket.close();
            }
        } catch (IOException e) {
            System.out.println(">>> [Hệ thống]: LỖI SERVER: " + e.getMessage());
        }
    }
}