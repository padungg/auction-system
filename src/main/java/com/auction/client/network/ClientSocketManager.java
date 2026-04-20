package com.auction.client.network;

import com.auction.client.util.LocalDateTimeAdapter;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

/**
 * Quản lý kết nối Socket TCP từ Client đến Server.
 *
 * Sử dụng Singleton pattern — toàn bộ app dùng chung 1 kết nối.
 * Giao tiếp bằng JSON theo protocol của Server:
 *   - Gửi: com.auction.model.protocol.Request (chứa RequestType + DTO payload)
 *   - Nhận: com.auction.model.protocol.Response (chứa ResponseStatus + message + payload)
 *
 * Protocol: DataOutputStream.writeUTF / DataInputStream.readUTF.
 */
public class ClientSocketManager {

    private static ClientSocketManager instance;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final Gson gson;

    private String host;
    private int port;

    private ClientSocketManager() {
        // Đăng ký LocalDateTimeAdapter vì AuctionDetailDTO có LocalDateTime
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    public static ClientSocketManager getInstance() {
        if (instance == null) {
            instance = new ClientSocketManager();
        }
        return instance;
    }

    /**
     * Mở kết nối TCP đến Server.
     *
     * @param host Địa chỉ server (vd: "localhost")
     * @param port Cổng server (vd: 8080)
     */
    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        System.out.println("[ClientSocketManager] Đã kết nối đến " + host + ":" + port);
    }

    /**
     * Gửi Request lên Server và đợi nhận Response.
     *
     * Luồng:
     * 1. Serialize Request (model.protocol.Request) → JSON string
     * 2. Gửi JSON qua DataOutputStream.writeUTF()
     * 3. Đọc phản hồi qua DataInputStream.readUTF()
     * 4. Deserialize JSON → Response (model.protocol.Response)
     * 5. Trả Response cho Controller
     *
     * @param request Request đã đóng gói (type + DTO payload)
     * @return Response từ Server
     */
    public Response sendRequest(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối đến Server!");
        }

        // 1. Serialize Request → JSON
        String jsonRequest = gson.toJson(request);
        System.out.println("[ClientSocketManager] >>> Gửi: " + jsonRequest);

        // 2. Gửi JSON (writeUTF)
        out.writeUTF(jsonRequest);
        out.flush();

        // 3. Đọc phản hồi (readUTF)
        String jsonResponse = in.readUTF();
        if (jsonResponse == null) {
            throw new IOException("Server đã đóng kết nối!");
        }
        System.out.println("[ClientSocketManager] <<< Nhận: " + jsonResponse);

        // 4. Deserialize JSON → Response
        return gson.fromJson(jsonResponse, Response.class);
    }

    /**
     * Đóng kết nối.
     */
    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[ClientSocketManager] Đã ngắt kết nối.");
        } catch (IOException e) {
            System.err.println("[ClientSocketManager] Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra kết nối còn sống không.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Lấy Gson instance (dùng chung để cast payload trong Controller).
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Lấy DataInputStream (cho ServerListener dùng khi cần lắng nghe push notification).
     */
    public DataInputStream getDataInputStream() {
        return in;
    }
}
