package com.example.backend.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Yagona xato formati (§52, §94).
 *
 * <pre>
 * { "code": "VALIDATION_ERROR", "message": "...", "errors": [ {"field": "...", "message": "..."} ] }
 * </pre>
 *
 * Frontend'ga stacktrace hech qachon yuborilmaydi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /** Mashina o'qiy oladigan kod: VALIDATION_ERROR, ACCESS_DENIED, CONTENT_NOT_FOUND ... */
    private String code;

    /** Odam o'qiy oladigan xabar. */
    private String message;

    /** Maydon bo'yicha xatolar. Validatsiyadan tashqarida null. */
    private List<FieldErrorItem> errors;

    private LocalDateTime timestamp;

    public ApiError(String code, String message) {
        this(code, message, null, LocalDateTime.now());
    }

    public ApiError(String code, String message, List<FieldErrorItem> errors) {
        this(code, message, errors, LocalDateTime.now());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldErrorItem {
        private String field;
        private String message;
    }
}
