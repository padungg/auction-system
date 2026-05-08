package com.auction.server.util;

public class ValidationUtils {
    public static void requireNonBlank(String val, String fieldName) {
        if (val == null || val.trim().isEmpty()) {
            throw new ValidationException(fieldName + " không được để trống");
        }
    }
}
