package com.pfe.DFinancialStatement.notification.service;

import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.notification.entity.Notification;
import com.pfe.DFinancialStatement.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    public void createNotification(User user, String message) {
        // Save notification in database
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notificationRepository.save(notification);

        // Send email to user
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String subject = "Notification - État de votre bilan";
            emailService.sendEmail(user.getEmail(), subject, message);
        }
    }

    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user);
    }

    public void markAllAsRead(User user) {
        List<Notification> notifications = getUnreadNotifications(user);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }
}
