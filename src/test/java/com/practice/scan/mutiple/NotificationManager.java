package com.practice.scan.mutiple;

import com.practice.diy.annotation.Autowired;
import com.practice.diy.annotation.Component;

@Component
public class NotificationManager {
    private final NotificationService notificationService;

    public NotificationManager(@Autowired(qualifier = "smsService") NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
