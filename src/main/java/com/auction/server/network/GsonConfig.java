package com.auction.server.network;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cấu hình Gson toàn cục cho Server.
 * Gson mặc định KHÔNG biết xử lý LocalDateTime → phải đăng ký adapter thủ công.
 * Dùng chung 1 instance duy nhất trong toàn bộ server để tối ưu hiệu năng.
 */
public class GsonConfig {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Tạo Gson instance đã được cấu hình sẵn.
     * Hỗ trợ serialize/deserialize LocalDateTime theo chuẩn ISO-8601.
     * Ví dụ: "2026-04-18T14:30:00"
     */
    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
                .setPrettyPrinting() // JSON dễ đọc khi debug
                .create();
    }

    /**
     * Chuyển LocalDateTime → JSON string.
     * VD: LocalDateTime.of(2026,4,18,14,30) → "2026-04-18T14:30:00"
     */
    private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(FORMATTER));
        }
    }

    /**
     * Chuyển JSON string → LocalDateTime.
     * VD: "2026-04-18T14:30:00" → LocalDateTime.of(2026,4,18,14,30)
     */
    private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }
}
