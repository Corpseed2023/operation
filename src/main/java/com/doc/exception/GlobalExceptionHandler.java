package com.doc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                ex.getErrorCode() != null ? ex.getErrorCode() : "ERR_NOT_FOUND"
        );
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                ex.getErrorCode() != null ? ex.getErrorCode() : "ERR_VALIDATION"
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

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