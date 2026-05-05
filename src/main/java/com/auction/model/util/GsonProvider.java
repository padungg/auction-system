package com.auction.model.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;

/**
 * SINGLETON — Gson instance dùng chung cho cả Client và Server.
 *
 * Tại sao Singleton?
 *   - Gson IMMUTABLE sau khi build → thread-safe hoàn toàn, không cần lock.
 *   - Server nhiều ClientHandler chạy song song: 1 instance shared → zero waste.
 *   - Client và Server dùng cùng 1 cấu hình → JSON format nhất quán 2 bên.
 *
 * Adapter đã đăng ký:
 *   - LocalDateTimeAdapter: serialize/deserialize java.time.LocalDateTime
 *     → Cần cho AuctionDetailDTO.startTime / .endTime
 *
 * Cách dùng:
 *   Gson gson = GsonProvider.getInstance();
 */
public class GsonProvider {

    private GsonProvider() {}

    /** Eager initialization — thread-safe nhờ JVM class loading. */
    private static final Gson INSTANCE = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    /** Trả về Gson instance dùng chung — thread-safe, zero allocation. */
    public static Gson getInstance() {
        return INSTANCE;
    }
}
