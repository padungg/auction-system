package com.auction.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
* Unit tests cho ValidationUtils.
* Kiểm tra requireNonBlank() và requireValidEmail().
*/
@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

  // requireNonBlank

  @Test
  @DisplayName("TC-VAL-01: requireNonBlank với chuỗi hợp lệ → không ném exception")
  void requireNonBlank_validString() {
    assertDoesNotThrow(() -> ValidationUtils.requireNonBlank("hello", "Field"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { "  ", "\t", "\n" })
  @DisplayName("TC-VAL-02: requireNonBlank với null/rỗng/whitespace → ném ValidationException")
  void requireNonBlank_invalidStrings(String value) {
    ValidationException ex = assertThrows(ValidationException.class,
        () -> ValidationUtils.requireNonBlank(value, "TestField"));
    assertTrue(ex.getMessage().contains("TestField"),
        "Message phải đề cập tên trường: " + ex.getMessage());
  }

  @Test
  @DisplayName("TC-VAL-03: requireNonBlank → message chứa tên field")
  void requireNonBlank_messageContainsFieldName() {
    ValidationException ex = assertThrows(ValidationException.class,
        () -> ValidationUtils.requireNonBlank(null, "Username"));
    assertTrue(ex.getMessage().contains("Username"));
  }

  // requireValidEmail

  @ParameterizedTest
  @ValueSource(strings = {
      "user@example.com",
      "user.name@domain.org",
      "a@b.vn",
      "user123@test-domain.net"
  })
  @DisplayName("TC-VAL-04: requireValidEmail với email hợp lệ → không ném exception")
  void requireValidEmail_valid(String email) {
    assertDoesNotThrow(() -> ValidationUtils.requireValidEmail(email),
        "Email hợp lệ không được ném exception: " + email);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "not-an-email",
      "missing@",
      "@nodomain.com",
      "no-at-sign",
      "user@.com",
      "user@domain.",
      "user@ space.com"
  })
  @DisplayName("TC-VAL-05: requireValidEmail với email không hợp lệ → ném ValidationException")
  void requireValidEmail_invalid(String email) {
    assertThrows(ValidationException.class,
        () -> ValidationUtils.requireValidEmail(email),
        "Email không hợp lệ phải ném exception: " + email);
  }

  @Test
  @DisplayName("TC-VAL-06: requireValidEmail với null → không ném exception (null được bỏ qua)")
  void requireValidEmail_null_noException() {
    // Theo code: if (email != null && ...) → null được bỏ qua
    assertDoesNotThrow(() -> ValidationUtils.requireValidEmail(null));
  }
}
