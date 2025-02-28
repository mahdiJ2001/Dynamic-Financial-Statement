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

    @Column(name = "form_data", columnDefinition = "jsonb")
    private String formData; // JSON contenant les valeurs remplies

    @Column(name = "analysis_result", columnDefinition = "jsonb")
    private String analysisResult; // Résultat de l'analyse après application des règles

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
