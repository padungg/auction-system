package com.auction.model.protocol;

public enum ResponseStatus {
    SUCCESS,        // Thành công
    ERROR,          // Lỗi
    UNAUTHORIZED,   // Không có quyền
    NOT_FOUND,      // Không thấy dữ liệu
    BAD_REQUEST     // Yêu cầu gửi sai
}
