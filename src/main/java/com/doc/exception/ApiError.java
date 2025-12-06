package com.doc.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

// ApiError.java
@Data
@AllArgsConstructor
public class ApiError {
    private HttpStatus status;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiError(HttpStatus status, String message, String errorCode) {
        this.status = status;
        this.message = message != null ? message : "No message provided";
        this.errorCode = errorCode;
    }
}