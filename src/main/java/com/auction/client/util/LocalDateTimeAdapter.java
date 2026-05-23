package com.auction.client.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <h2>LocalDateTimeAdapter</h2>
 * <p>
 * Bộ chuyển đổi dữ liệu cấu trúc thời gian Java 8 dành cho Gson (Custom Gson Serializer/Deserializer Adapter for java.time.LocalDateTime).
 * Hiện thực hóa đồng thời hai giao diện {@link JsonSerializer} và {@link JsonDeserializer} để tạo lập bộ lọc dòng dữ liệu hai chiều an toàn.
 * </p>
 * * <p><b>Tầm quan trọng trong kiến trúc hệ thống:</b></p>
 * <ul>
 * <li><b>Khắc phục hạn chế lõi của thư viện (Gson Core Bypass):</b> Thư viện Gson mặc định không cấu hình sẵn cơ chế tuần tự hóa (Serialization) cho các lớp thời gian hiện đại thuộc gói `java.time.*`. Thành phần này là bắt buộc để tránh lỗi sập luồng hệ thống hoặc sai lệch dữ liệu khi truyền tải bản tin JSON.</li>
 * <li><b>Đồng bộ hóa dữ liệu phiên đấu giá (Auction State Synchronization):</b> Đóng vai trò then chốt trong việc phân tích cú pháp (Parsing) các gói tin DTO phức tạp (ví dụ: `AuctionDetailDTO`, `BidHistoryDTO`) chứa các mốc thời gian cốt lõi như thời điểm mở thưởng (`startTime`), thời hạn đếm ngược chống bắn tỉa (`endTime`).</li>
 * </ul>
 * * @since 1.0
 * @see com.google.gson.JsonSerializer
 * @see com.google.gson.JsonDeserializer
 * @see java.time.LocalDateTime
 * @see java.time.format.DateTimeFormatter
 */
public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    /** * Định dạng chuỗi ký tự thời gian tiêu chuẩn quốc tế ISO-8601 (Ví dụ biểu diễn cấu trúc chuỗi mạng: 2026-05-23T17:45:00).
     * Được thiết lập ở dạng hằng số tĩnh (`static final`) giúp tối ưu hiệu năng luồng, tránh việc khởi tạo lặp đi lặp lại.
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Chuyển đổi đối tượng cấu trúc Java LocalDateTime sang phần tử nguyên thủy JSON dạng chuỗi ký tự (Mã hóa đầu ra - Serialize).
     * <p>
     * Hàm tự động trích xuất cấu trúc dữ liệu vùng nhớ RAM của đối tượng thời gian sang chuỗi văn bản thuần dạng
     * ISO-8601 để đóng gói an toàn vào luồng truyền tải dữ liệu Socket mạng.
     * </p>
     * * @param src       Đối tượng dữ liệu thời gian gốc {@link LocalDateTime} cần chuyển đổi
     * @param typeOfSrc Định dạng kiểu cấu trúc động của thực thể nguồn phục vụ ánh xạ lớp
     * @param context   Ngữ cảnh tuần tự hóa tập trung nội bộ của thư viện Gson
     * @return {@link JsonElement} Đối tượng JSON dạng chuỗi nguyên thủy {@link JsonPrimitive} chứa mốc thời gian đã định dạng
     */
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        // Biến đổi cấu trúc ngày giờ sang chuỗi văn bản thuần dựa trên bộ lọc tiêu chuẩn ISO
        return new JsonPrimitive(src.format(FORMATTER));
    }

    /**
     * Chuyển đổi phần tử JSON dạng chuỗi văn bản mạng ngược về đối tượng vùng nhớ Java LocalDateTime (Giải mã đầu vào - Deserialize).
     * <p>
     * Tiếp nhận chuỗi JSON nguyên thủy từ luồng đọc Socket mạng, bóc tách chuỗi văn bản
     * và chuyển đổi cấu trúc ngữ nghĩa về thực thể thời gian Java 8 để thực hiện các phép toán so sánh, đếm ngược đồ họa.
     * </p>
     * * @param json      Thành phần phần tử dữ liệu JSON gốc {@link JsonElement} trích xuất từ chuỗi gói tin mạng
     * @param typeOfT   Kiểu dữ liệu đích mong muốn ánh xạ cấu trúc ngược (`LocalDateTime`)
     * @param context   Ngữ cảnh giải mã tuần tự hóa tập trung nội bộ của thư viện Gson
     * @return {@link LocalDateTime} Thực thể thời gian Java 8 khôi phục hoàn chỉnh
     * @throws JsonParseException Ngoại lệ hệ thống ném ra nếu chuỗi văn bản JSON đầu vào không tuân thủ chính xác bộ lọc định dạng ISO tiêu chuẩn
     */
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        // Thực hiện phân tích cú pháp chuỗi mạng ép kiểu về cấu trúc thực thể LocalDateTime Java 8
        return LocalDateTime.parse(json.getAsString(), FORMATTER);
    }
}