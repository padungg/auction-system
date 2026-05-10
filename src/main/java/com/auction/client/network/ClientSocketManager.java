package com.auction.client.network;

import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Quản lý kết nối Socket TCP từ Client đến Server.
 *
 * Sử dụng Singleton pattern.
 */
public class ClientSocketManager {

    private static ClientSocketManager instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;

    private String host;
    private int port;

    // Hàng đợi để nhận Response cho Request đồng bộ (chỉ 1 request tại 1 thời điểm)
    private final BlockingQueue<Response> responseQueue = new ArrayBlockingQueue<>(1);

    // Callback cho các push notification (real-time)
    private Consumer<JsonObject> notificationListener;
    private Thread listenerThread;

    private ClientSocketManager() {
        this.gson = GsonProvider.getInstance();
    }

    public static ClientSocketManager getInstance() {
        if (instance == null) {
            instance = new ClientSocketManager();
        }
        return instance;
    }

    public void setNotificationListener(Consumer<JsonObject> listener) {
        this.notificationListener = listener;
    }

    public void connect(String host, int port) throws IOException {
        if (isConnected()) return;

        this.host = host;
        this.port = port;

        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        System.out.println("[ClientSocketManager] Đã kết nối đến " + host + ":" + port);

        // Khởi động luồng đọc liên tục
        startListenerThread();
    }

    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("[ClientSocketManager] <<< Nhận: " + line);
                    if (line.contains("\"event\"")) {
                        // Đây là Push Notification từ Server
                        JsonObject push = gson.fromJson(line, JsonObject.class);
                        if (notificationListener != null) {
                            notificationListener.accept(push);
                        }
                    } else {
                        // Đây là Response cho 1 Request
                        Response response = gson.fromJson(line, Response.class);
                        responseQueue.offer(response);
                    }
                }
            } catch (IOException e) {
                if (isConnected()) {
                    System.err.println("[ClientSocketManager] Lỗi đọc dữ liệu: " + e.getMessage());
                }
            }
            System.out.println("[ClientSocketManager] Luồng đọc đã dừng.");
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public synchronized Response sendRequest(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối đến Server!");
        }

        responseQueue.clear(); // Xóa response cũ nếu có

        String jsonRequest = gson.toJson(request);
        System.out.println("[ClientSocketManager] >>> Gửi: " + jsonRequest);
        out.println(jsonRequest);

        try {
            // Đợi response tối đa 10 giây
            Response response = responseQueue.poll(10, TimeUnit.SECONDS);
            if (response == null) {
                throw new IOException("Timeout: Server không phản hồi");
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Bị gián đoạn khi đợi server", e);
        }
    }

    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (listenerThread != null) listenerThread.interrupt();
            System.out.println("[ClientSocketManager] Đã ngắt kết nối.");
        } catch (IOException e) {
            System.err.println("[ClientSocketManager] Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public Gson getGson() {
        return gson;
    }
}
