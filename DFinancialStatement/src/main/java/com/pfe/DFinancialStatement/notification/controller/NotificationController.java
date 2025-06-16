package com.pfe.DFinancialStatement.notification.controller;

import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.AuthService;
import com.pfe.DFinancialStatement.notification.entity.Notification;
import com.pfe.DFinancialStatement.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    @Autowired
    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    /**
     * Get all unread notifications for the currently authenticated user.
     * Example: GET /api/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        User currentUser = authService.getCurrentUser();
        List<Notification> notifications = notificationService.getUnreadNotifications(currentUser);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Mark all unread notifications for the currently authenticated user as read.
     * Example: PUT /api/notifications/mark-as-read
     */
    @PutMapping("/mark-as-read")
    public ResponseEntity<Void> markAllAsRead() {
        User currentUser = authService.getCurrentUser();
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok().build();
    }
}
