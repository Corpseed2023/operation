package com.doc.notification;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDto {

    private Long id;

    private Long receiverId;

    private Long actorId;

    private String actorName;

    private NotificationModule module;

    private NotificationEventType eventType;

    private Long referenceId;

    private String referenceNumber;

    private String title;

    private String message;

    private String redirectUrl;

    private NotificationPriority priority;

    private NotificationDisplayType displayType;

    private String metadataJson;

    private Boolean read;

    private Boolean deleted;

    private LocalDateTime createdAt;
}