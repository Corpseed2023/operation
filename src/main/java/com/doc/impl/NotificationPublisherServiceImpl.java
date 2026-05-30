package com.doc.impl;

import com.doc.notification.NotificationClient;
import com.doc.notification.NotificationCreateRequestDto;
import com.doc.service.NotificationPublisherService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisherServiceImpl implements NotificationPublisherService {

    private final NotificationClient notificationClient;

    @Override
    public void sendNotification(NotificationCreateRequestDto requestDto) {
        try {
            if (requestDto == null || requestDto.getReceiverId() == null) {
                log.warn("Notification skipped because request or receiverId is null");
                return;
            }

            notificationClient.createNotification(requestDto);

            log.info(
                    "Notification sent successfully | receiverId={} | module={} | eventType={} | referenceId={}",
                    requestDto.getReceiverId(),
                    requestDto.getModule(),
                    requestDto.getEventType(),
                    requestDto.getReferenceId()
            );

        } catch (FeignException ex) {
            log.error(
                    "Notification-service Feign error | status={} | receiverId={} | eventType={} | message={}",
                    ex.status(),
                    requestDto != null ? requestDto.getReceiverId() : null,
                    requestDto != null ? requestDto.getEventType() : null,
                    ex.getMessage()
            );

        } catch (Exception ex) {
            log.error(
                    "Unexpected error while sending notification | receiverId={} | eventType={} | message={}",
                    requestDto != null ? requestDto.getReceiverId() : null,
                    requestDto != null ? requestDto.getEventType() : null,
                    ex.getMessage(),
                    ex
            );
        }
    }
}