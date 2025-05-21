package com.pfe.DFinancialStatement.dmn_rule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionEvaluationResult {
    private String expression;
    private String condition;
    private boolean result;
    private String messageErreur;
    private String severite;
    private Double evaluatedValue;
}
