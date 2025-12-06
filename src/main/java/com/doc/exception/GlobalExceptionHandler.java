// src/main/java/com/doc/exception/GlobalExceptionHandler.java

package com.doc.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Resource Not Found → 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                ex.getErrorCode() != null ? ex.getErrorCode() : "ERR_NOT_FOUND"
        );
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    // 2. Business Validation Errors → 400
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                ex.getErrorCode() != null ? ex.getErrorCode() : "ERR_VALIDATION"
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    // 3. IllegalArgumentException (e.g., duplicate milestone order) → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                "ERR_INVALID_REQUEST"
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Invalid request payload";
        String errorCode = "ERR_INVALID_INPUT";

        if (ex.getCause() instanceof InvalidFormatException ife) {
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String fieldName = ife.getPath().isEmpty()
                        ? "field"
                        : ife.getPath().get(0).getFieldName();

                String invalidValue = ife.getValue() != null
                        ? ife.getValue().toString()
                        : "null";

                String validValues = String.join(", ", Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString)
                        .toList());

                message = String.format(
                        "Invalid value '%s' for '%s'. Allowed values are: [%s]",
                        invalidValue, fieldName, validValues
                );
                errorCode = "ERR_INVALID_ENUM_VALUE";
            } else {
                message = "Invalid format for field: " +
                        (ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName());
            }
        }

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, message, errorCode);
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    // 5. Final Fallback – Real unexpected errors → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {

        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                "ERR_INTERNAL"
        );
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}