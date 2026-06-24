package com.doc.dto.document;

import com.doc.em.DocumentExpiryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentExpiryResponseDto(
        String fileName,
        LocalDate expiryDate,
        DocumentExpiryStatus status,
        String matchedText,
        Boolean manualReviewRequired,
        String message,
        LocalDateTime checkedAt
) {
}