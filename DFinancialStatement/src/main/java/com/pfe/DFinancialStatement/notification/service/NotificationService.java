package com.pfe.DFinancialStatement.notification.service;

import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.notification.entity.Notification;
import com.pfe.DFinancialStatement.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    public void createNotification(User user, String messageKey, Map<String, String> params) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessageKey(messageKey);
        notification.setParams(params);
        notificationRepository.save(notification);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String subject = "Notification - État de votre bilan";
            // For simplicity, just send messageKey + params in email or implement server-side translation
            String emailBody = "Message key: " + messageKey + ", params: " + params.toString();
            emailService.sendEmail(user.getEmail(), subject, emailBody);
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
