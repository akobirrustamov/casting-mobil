package com.example.backend.Config;

import com.example.backend.exceptions.InvalidStudentDataException;
import com.example.backend.exceptions.ResourceNotFoundException;
import com.example.backend.exceptions.StudentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<?> handleStudentNotFound(StudentNotFoundException ex) {
        log.error("Student not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStudentDataException.class)
    public ResponseEntity<?> handleInvalidStudentData(InvalidStudentDataException ex) {
        log.warn("Invalid student data: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationError(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("VALIDATION_ERROR", "Invalid input data", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> buildErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    private Map<String, Object> buildErrorResponse(String error, String message, Map<String, String> details) {
        Map<String, Object> response = buildErrorResponse(error, message);
        response.put("details", details);
        return response;
    }
}

