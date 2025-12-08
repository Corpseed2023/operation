// src/main/java/com/doc/exception/GlobalExceptionHandler.java

package com.doc.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getErrorCode());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // NEW: Handle IllegalArgumentException with fallback message
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "Invalid request parameters";
        }
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, message, "ERR_INVALID_REQUEST");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // NEW: Handle @Valid bean validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid input data");
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, message, "ERR_VALIDATION_FAILED");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // NEW: Handle JSON parsing errors (enum, date, etc.)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Invalid request format";
        String errorCode = "ERR_INVALID_INPUT";

        if (ex.getCause() instanceof InvalidFormatException ife) {
            String field = ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName();
            String value = Objects.toString(ife.getValue(), "null");
            if (ife.getTargetType().isEnum()) {
                String allowed = String.join(", ", Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString).toList());
                message = String.format("Invalid value '%s' for '%s'. Allowed values: [%s]", value, field, allowed);
                errorCode = "ERR_INVALID_ENUM_VALUE";
            } else {
                message = String.format("Invalid format for '%s': %s", field, value);
            }
        }

        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, message, errorCode);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Final fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex) {
        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                "ERR_INTERNAL"
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}