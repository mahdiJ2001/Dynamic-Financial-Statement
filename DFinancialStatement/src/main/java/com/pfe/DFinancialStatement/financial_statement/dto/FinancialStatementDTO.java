package com.pfe.DFinancialStatement.financial_statement.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FinancialStatementDTO {
    private String formData;
    private byte[] report;
    private LocalDateTime createdAt;
}
