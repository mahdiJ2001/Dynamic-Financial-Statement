package com.pfe.DFinancialStatement.activity.entity;

import com.pfe.DFinancialStatement.activity.enums.ActionType;
import com.pfe.DFinancialStatement.auth.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private String description;

    private LocalDateTime timestamp = LocalDateTime.now();
}
