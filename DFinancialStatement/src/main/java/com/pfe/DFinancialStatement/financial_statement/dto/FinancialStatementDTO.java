package com.pfe.DFinancialStatement.financial_statement.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FinancialStatementDTO {
    private String formData; // Données saisies par l'utilisateur (JSON)
    private String analysisResult; // Résultat après application des règles (JSON)
    private LocalDateTime createdAt;
}
