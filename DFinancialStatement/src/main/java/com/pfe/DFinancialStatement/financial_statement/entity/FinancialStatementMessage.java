package com.pfe.DFinancialStatement.financial_statement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pfe.DFinancialStatement.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_statement_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "financial_statement_id", nullable = false)
    @JsonIgnore
    private FinancialStatement financialStatement;


    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false)
    private String messageContent;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
}
