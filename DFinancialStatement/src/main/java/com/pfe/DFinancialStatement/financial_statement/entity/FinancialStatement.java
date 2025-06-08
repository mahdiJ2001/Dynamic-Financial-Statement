package com.pfe.DFinancialStatement.financial_statement.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pfe.DFinancialStatement.auth.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "report")
    private byte[] report;


    @Column(name = "company_name")
    private String companyName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatementStatus status = StatementStatus.PENDING;

    @Column(name = "evaluation_result", columnDefinition = "TEXT")
    private String evaluationResult;

    @OneToMany(mappedBy = "financialStatement", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<FinancialStatementMessage> messages = new ArrayList<>();


}
