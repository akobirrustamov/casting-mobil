package com.example.backend.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Biznes qoidasi buzilganda tashlanadi. Kod va HTTP status o'zi bilan yuradi,
 * shuning uchun GlobalExceptionHandler uni tarjimasiz qaytara oladi.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException notFound(String entity, Object id) {
        return new BusinessException(
                entity.toUpperCase() + "_NOT_FOUND",
                entity + " topilmadi: " + id,
                HttpStatus.NOT_FOUND);
    }

    public static BusinessException accessDenied(String message) {
        return new BusinessException("ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException duplicate(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    public static BusinessException validation(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
