package com.doc.notification;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequestDto {

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
}