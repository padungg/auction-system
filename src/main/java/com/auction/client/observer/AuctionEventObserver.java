package com.auction.client.observer;

import com.google.gson.JsonObject;

/**
 * <h2>AuctionEventObserver</h2>
 * <p>
 * Giao diện đăng ký và đón nhận sự kiện đấu giá thời gian thực (Real-time Auction Event Observer Interface).
 * Đóng vai trò là thành phần định nghĩa hợp đồng hành vi (Contract Interface) trong mô hình kiến trúc <b>Observer Pattern</b>.
 * </p>
 * * <p><b>Kiến trúc vận hành hệ thống:</b></p>
 * <ul>
 * <li><b>Cơ chế đăng ký (Subscription Model):</b> Mỗi phân hệ màn hình đồ họa (như `MainController`, `AuctionDetailController`) có nhu cầu tiếp nhận các tín hiệu biến động trạng thái bất đồng bộ từ Server đều phải triển khai (implement) giao diện này và đăng ký thực thể thông qua `ClientSocketManager`.</li>
 * <li><b>An toàn luồng UI (UI Thread Safety):</b> Để đảm bảo an toàn cho luồng đồ họa JavaFX, các phương thức hiện thực hóa giao diện này luôn được điều phối chạy đồng bộ trực tiếp trên <i>JavaFX Application Thread</i> thông qua cơ chế `Platform.runLater`.</li>
 * </ul>
 * * <p><b>Các danh mục sự kiện (Event Matrix) hệ thống đang hỗ trợ phân phối:</b></p>
 * <ul>
 * <li>{@code "BID_UPDATE"}: Phát tín hiệu khi có một thành viên bất kỳ thực hiện đặt mức giá mới (New Bid Placement) thành công cho sản phẩm.</li>
 * <li>{@code "ANTI_SNIPE"}: Phát tín hiệu kéo dài thời hạn đếm ngược của phòng phiên đấu giá nhằm chống hành vi ôm giá, bắn tỉa phút chót (Snipping Protection).</li>
 * <li>{@code "AUCTION_CLOSED"}: Phát tín hiệu thông báo phiên đấu giá hiện hành đã chính thức đóng, khóa sổ và kết thúc chu kỳ kinh doanh.</li>
 * </ul>
 * * @since 1.0
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.client.network.AuctionEventDispatcher
 */
public interface AuctionEventObserver {

    /**
     * Hàm phản hồi tự động (Callback Method) khi máy chủ Server thực hiện lệnh Push dữ liệu gói tin sự kiện về phía Client.
     * <p>
     * Hàm sẽ tự động bóc tách tên sự kiện để điều phối render đồ họa, làm mới bộ đếm ngược thời gian hoặc bắn nhãn
     * thông báo đẩy (Real-time Toast Notification) lên màn hình trạm của người dùng.
     * </p>
     * * @param event     Tên mã máy hoặc mã định danh phân loại của sự kiện hệ thống (Ví dụ: {@code "BID_UPDATE"}, {@code "AUCTION_CLOSED"})
     * @param auctionId Mã định danh duy nhất (UUID/ID chuỗi văn bản) của phiên đấu giá phát sinh biến động
     * @param payload   Khối thực thể dữ liệu định dạng JSON chứa các thuộc tính mở rộng đi kèm (Ví dụ: thông tin người đặt giá, mức giá mới, mốc thời gian kết thúc mới...)
     */
    void onAuctionEvent(String event, String auctionId, JsonObject payload);
}