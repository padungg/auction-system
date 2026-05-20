package com.auction.client.observer;

import com.google.gson.JsonObject;

/**
 * Giao diện Người lắng nghe Sự kiện Đấu giá (Auction Event Observer Interface).
 * Đóng vai trò là thành phần cốt lõi trong cơ chế cập nhật dữ liệu thời gian thực (Real-time Update)
 * theo mô hình kiến trúc Observer Pattern.
 * * Mỗi phân hệ màn hình đồ họa (Controller) có nhu cầu tiếp nhận các tín hiệu thay đổi trạng thái bất đồng bộ
 * từ phía hệ thống máy chủ đều phải triển khai (implement) giao diện này và thực hiện đăng ký thực thể
 * thông qua bộ quản lý kết nối ClientSocketManager.
 * * Các danh mục sự kiện hệ thống đang hỗ trợ bộ lọc:
 * - "BID_UPDATE"     : Phát tín hiệu khi có người dùng vừa thực hiện đặt mức giá mới thành công.
 * - "ANTI_SNIPE"     : Phát tín hiệu kéo dài thời hạn đếm ngược của phiên đấu giá nhằm chống hành vi ôm giá phút chót.
 * - "AUCTION_CLOSED" : Phát tín hiệu thông báo phiên đấu giá hiện hành đã chính thức đóng và kết thúc.
 */
public interface AuctionEventObserver {

    /**
     * Phương thức phản hồi tự động khi máy chủ Server thực hiện cấu trúc lệnh Push dữ liệu sự kiện về phía Client.
     * Để đảm bảo an toàn cho luồng đồ họa, phương thức này luôn được chỉ định thực thi đồng bộ
     * trực tiếp trên JavaFX UI Thread thông qua cơ chế Platform.runLater.
     *
     * @param event     Tên hoặc mã định danh của sự kiện hệ thống (Ví dụ: "BID_UPDATE", "AUCTION_CLOSED")
     * @param auctionId Mã định danh duy nhất (UUID/ID) của phiên đấu giá xảy ra biến động
     * @param payload   Khối dữ liệu định dạng JSON chứa các thuộc tính mở rộng đi kèm (Ví dụ: mức giá mới, thời gian kết thúc mới...)
     */
    void onAuctionEvent(String event, String auctionId, JsonObject payload);
}