package com.auction.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private static final int port = 8080;
    private static final int maxClients = 20;
    public static void main(String[] args) {
        // Tạo 20 luồng hoạt động
        ExecutorService executor = Executors.newFixedThreadPool(maxClients);

    }
}