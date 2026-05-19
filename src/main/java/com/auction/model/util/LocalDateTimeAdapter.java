package com.auction.model.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson adapter cho java.time.LocalDateTime.
 *
 * Đặt trong com.auction.model.util vì cả Client và Server đều cần:
 *   - Client: ClientSocketManager dùng khi gửi/nhận Request/Response chứa LocalDateTime
 *   - Server: ClientHandler dùng khi parse Request và serialize Response
 *
 * Nếu đặt riêng 2 bên → nguy cơ cấu hình khác nhau → lỗi parse JSON.
 * Đặt chung → 1 nguồn sự thật, đảm bảo format nhất quán: ISO_LOCAL_DATE_TIME
 *              (VD: "2026-05-03T20:30:00")
 */
public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.format(FORMATTER));
    }

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return LocalDateTime.parse(json.getAsString(), FORMATTER);
    }
}
