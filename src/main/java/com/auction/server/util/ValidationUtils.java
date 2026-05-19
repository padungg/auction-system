package com.auction.server.util;

public class ValidationUtils {
    public static void requireNonBlank(String val, String fieldName) {
        if (val == null || val.trim().isEmpty()) {
            throw new ValidationException(fieldName + " không được để trống");
        }
    }

    public static void requireValidEmail(String email) {
        if (email != null && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new ValidationException("Email không hợp lệ");
        }
    }
}
