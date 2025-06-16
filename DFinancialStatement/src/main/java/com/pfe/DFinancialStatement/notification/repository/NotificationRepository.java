package com.pfe.DFinancialStatement.notification.repository;

import com.pfe.DFinancialStatement.notification.entity.Notification;
import com.pfe.DFinancialStatement.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndIsReadFalse(User user);
}
