package com.example.backend.exceptions;

public class InvalidStudentDataException extends RuntimeException {
    public InvalidStudentDataException(String message) {
        super(message);
    }

    public InvalidStudentDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

