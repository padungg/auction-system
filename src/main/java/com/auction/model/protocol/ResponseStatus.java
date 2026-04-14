package com.auction.model.protocol;

/**
 * Định nghĩa các mã trạng thái trả về từ Server xuống Client.
 */
public enum ResponseStatus {
    SUCCESS,        // Xử lý thành công
    ERROR,          // Lỗi chung (Lỗi máy chủ hoặc logic nghiệp vụ từ chối)
    UNAUTHORIZED,   // Lỗi quyền hạn (Chưa đăng nhập, token hết biên độ, hoặc sai mật khẩu)
    NOT_FOUND,      // Lỗi không tìm thấy (VD: Không tìm thấy ID của phiên đấu giá)
    BAD_REQUEST     // Lỗi cú pháp (VD: Client gửi thiếu field bắt buộc, field sai định dạng)
}
