package com.doc.dto.legalDashbaord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingLegalQueueItemDto {
    private String type;      // PROJECT | VENDOR | COMPANY | PAYMENT
    private Long id;
    private String title;
    private String subtitle;
    private LocalDateTime date;
}