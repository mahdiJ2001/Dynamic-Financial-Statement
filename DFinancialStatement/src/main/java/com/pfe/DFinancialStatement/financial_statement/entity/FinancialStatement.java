package com.pfe.DFinancialStatement.financial_statement.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

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
    private String formData;  // Form data stored as String (JSON)

    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult; // Analysis result stored as String (JSON)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
