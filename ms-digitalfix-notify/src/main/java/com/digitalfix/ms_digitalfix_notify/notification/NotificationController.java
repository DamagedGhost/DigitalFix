package com.digitalfix.ms_digitalfix_notify.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
public class NotificationController {

    private final NotificationHistoryService historyService;

    public NotificationController(NotificationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<NotificationRecord> list() {
        return historyService.getRecentNotifications();
    }
}
