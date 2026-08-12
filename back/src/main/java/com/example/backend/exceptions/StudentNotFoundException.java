package com.example.backend.exceptions;

import java.util.UUID;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(UUID id) {
        super("Student not found with id: " + id);
    }

    public StudentNotFoundException(String message) {
        super(message);
    }
}

