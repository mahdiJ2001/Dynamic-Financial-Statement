package com.pfe.DFinancialStatement.financial_statement.entity;

import com.pfe.DFinancialStatement.auth.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import com.pfe.DFinancialStatement.financial_statement.entity.StatementStatus;

@Entity
@Table(name = "financial_statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_data", columnDefinition = "TEXT")
    private String formData;

    @Lob
    @Column(name = "report")
    private byte[] report;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatementStatus status = StatementStatus.PENDING;

    @Column(name = "rejection_cause")
    private String rejectionCause;
}
