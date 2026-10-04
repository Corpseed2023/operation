package com.doc.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger =
            LogManager.getLogger(GlobalExceptionHandler.class);

    // =====================================================================
    // RESOURCE NOT FOUND
    // =====================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex
    ) {

        logger.warn(
                "Resource not found. errorCode={}, message={}",
                ex.getErrorCode(),
                ex.getMessage()
        );

        ApiError error =
                new ApiError(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage(),
                        ex.getErrorCode()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    // =====================================================================
    // BUSINESS / VALIDATION ERROR
    // =====================================================================

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(
            ValidationException ex
    ) {

        logger.warn(
                "Request validation failed. errorCode={}, message={}",
                ex.getErrorCode(),
                ex.getMessage()
        );

        ApiError error =
                new ApiError(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        ex.getErrorCode()
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // =====================================================================
    // ILLEGAL ARGUMENT
    // =====================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {

        String message = ex.getMessage();

        if (message == null || message.trim().isEmpty()) {
            message = "Invalid request parameters.";
        }

        logger.warn(
                "Invalid request argument. message={}",
                message
        );

        ApiError error =
                new ApiError(
                        HttpStatus.BAD_REQUEST,
                        message,
                        "ERR_INVALID_REQUEST"
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // =====================================================================
    // DTO VALIDATION
    // =====================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex
    ) {

        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                error.getField()
                                        + " "
                                        + error.getDefaultMessage()
                        )
                        .findFirst()
                        .orElse(
                                "Invalid input data."
                        );

        logger.warn(
                "DTO validation failed. message={}",
                message
        );

        ApiError error =
                new ApiError(
                        HttpStatus.BAD_REQUEST,
                        message,
                        "ERR_VALIDATION_FAILED"
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // =====================================================================
    // INVALID JSON / ENUM
    // =====================================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex
    ) {

        String message =
                "Invalid request format.";

        String errorCode =
                "ERR_INVALID_INPUT";

        if (ex.getCause()
                instanceof InvalidFormatException invalidFormatException) {

            String field =
                    invalidFormatException
                            .getPath()
                            .isEmpty()
                            ? "unknown"
                            : invalidFormatException
                            .getPath()
                            .get(0)
                            .getFieldName();

            String value =
                    Objects.toString(
                            invalidFormatException.getValue(),
                            "null"
                    );

            if (invalidFormatException
                    .getTargetType()
                    .isEnum()) {

                String allowed =
                        String.join(
                                ", ",
                                Arrays.stream(
                                                invalidFormatException
                                                        .getTargetType()
                                                        .getEnumConstants()
                                        )
                                        .map(Object::toString)
                                        .toList()
                        );

                message =
                        String.format(
                                "Invalid value '%s' for field '%s'. "
                                        + "Allowed values are: [%s]",
                                value,
                                field,
                                allowed
                        );

                errorCode =
                        "ERR_INVALID_ENUM_VALUE";

            } else {

                message =
                        String.format(
                                "Invalid value or format for field '%s': %s",
                                field,
                                value
                        );
            }
        }

        logger.warn(
                "Request body could not be parsed. errorCode={}, message={}",
                errorCode,
                message
        );

        ApiError error =
                new ApiError(
                        HttpStatus.BAD_REQUEST,
                        message,
                        errorCode
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // =====================================================================
    // UNEXPECTED ERROR
    // =====================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(
            Exception ex
    ) {

        /*
         * Full exception remains in server logs.
         * Internal technical details are NOT exposed to frontend users.
         */
        logger.error(
                "Unexpected server error occurred",
                ex
        );

        ApiError error =
                new ApiError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected server error occurred. "
                                + "Please try again or contact support if the problem continues.",
                        "ERR_INTERNAL"
                );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}