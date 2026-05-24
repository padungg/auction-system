package com.auction.client.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bộ chuyển đổi định dạng LocalDateTime cho thư viện Gson.
 * Giúp serialize và deserialize đối tượng LocalDateTime sang định dạng chuỗi ISO_LOCAL_DATE_TIME chuẩn và ngược lại.
 */
@SuppressWarnings("unused")
public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Chuyển đổi đối tượng LocalDateTime thành phần tử JSON (JsonPrimitive chuỗi).
     */
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.format(FORMATTER));
    }

    /**
     * Khôi phục đối tượng LocalDateTime từ chuỗi JSON.
     */
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return LocalDateTime.parse(json.getAsString(), FORMATTER);
    }
}