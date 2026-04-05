package com.auction.server.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class UserDAOImpl {

    // Hàm này sẽ mở file database.txt để tìm xem user/pass có đúng không
    public boolean checkLogin(String user, String pass) {
        try {
            // "Máy" sẽ đi tìm file database.txt nằm ở ngoài cùng dự án
            File file = new File("database.txt");
            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {
                String data = reader.nextLine(); // Đọc 1 dòng, ví dụ: admin,123,Admin
                String[] parts = data.split(","); // Chia làm các phần dựa trên dấu phẩy

                // parts[0] là username, parts[1] là password
                if (parts.length >= 2) {
                    if (parts[0].trim().equals(user) && parts[1].trim().equals(pass)) {
                        reader.close();
                        return true; // Khớp rồi!
                    }
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("LỖI: Không tìm thấy file database.txt ở thư mục gốc!");
        }
        return false; // Không tìm thấy dòng nào khớp
    }
}