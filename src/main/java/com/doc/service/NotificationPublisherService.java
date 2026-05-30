package com.doc.service;


import com.doc.notification.NotificationCreateRequestDto;

public interface NotificationPublisherService {

    void sendNotification(NotificationCreateRequestDto requestDto);
}