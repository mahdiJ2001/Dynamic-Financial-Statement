package com.pfe.DFinancialStatement.outbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;
    private boolean processed = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
