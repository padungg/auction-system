package com.auction.client.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bộ chuyển đổi dữ liệu Gson dành cho kiểu LocalDateTime (Gson Adapter for java.time.LocalDateTime).
 * * Thành phần này đóng vai trò bắt buộc do thư viện Gson mặc định không cấu hình sẵn cơ chế mã hóa/giải mã (Serialize/Deserialize)
 * cho cấu trúc dữ liệu thời gian hiện đại java.time.LocalDateTime của Java 8.
 * * Adapter này trực tiếp phục vụ cho việc đồng bộ các đối tượng DTO phức tạp (Ví dụ: AuctionDetailDTO) chứa các trường
 * thuộc tính thời gian như thời điểm kích hoạt (startTime) và thời điểm kết thúc phiên (endTime).
 */
public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    /** Định dạng chuỗi thời gian tiêu chuẩn ISO-8601 (Ví dụ: 2026-05-20T22:45:00) phục vụ truyền tải gói tin JSON */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Chuyển đổi đối tượng Java LocalDateTime sang phần tử dữ liệu JSON dạng chuỗi ký tự (Mã hóa - Serialize).
     */
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.format(FORMATTER));
    }

    /**
     * Chuyển đổi phần tử dữ liệu JSON dạng chuỗi văn bản ngược về đối tượng cấu trúc Java LocalDateTime (Giải mã - Deserialize).
     *
     * @throws JsonParseException Ngoại lệ phát sinh nếu chuỗi JSON đầu vào không tuân thủ đúng định dạng thời gian ISO tiêu chuẩn
     */
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return LocalDateTime.parse(json.getAsString(), FORMATTER);
    }
}