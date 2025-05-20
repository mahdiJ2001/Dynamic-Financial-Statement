package com.pfe.DFinancialStatement.financial_statement.dto;

import com.pfe.DFinancialStatement.dmn_rule.dto.ExpressionEvaluationResult;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

import com.pfe.DFinancialStatement.financial_statement.entity.StatementStatus;

@Data
public class FinancialStatementDTO {
    private Long id;
    private String formData;
    private byte[] report;
    private LocalDateTime createdAt;
    private String contributorName;
    private StatementStatus status;
    private String rejectionCause;
    private String companyName;
    private List<ExpressionEvaluationResult> evaluationResults;
}
