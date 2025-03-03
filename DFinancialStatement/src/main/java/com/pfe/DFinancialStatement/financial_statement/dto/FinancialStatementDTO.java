package com.pfe.DFinancialStatement.financial_statement.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FinancialStatementDTO {
    private String formData;       // Form data stored as String (JSON)
    private String analysisResult; // Analysis result stored as String (JSON)
    private LocalDateTime createdAt;
}
