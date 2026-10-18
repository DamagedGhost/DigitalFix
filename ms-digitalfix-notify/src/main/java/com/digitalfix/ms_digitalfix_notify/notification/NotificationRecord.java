package com.digitalfix.ms_digitalfix_notify.notification;

public record NotificationRecord(
        String id,
        String message,
        String timestamp,
        String type
) {}
