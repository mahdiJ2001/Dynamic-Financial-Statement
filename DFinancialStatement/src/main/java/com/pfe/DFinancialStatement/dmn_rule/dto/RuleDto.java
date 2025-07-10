package com.pfe.DFinancialStatement.dmn_rule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleDto {
    private String expression;
    private String condition;
    private String value;
    private String messageErreur;
    private String severite;
}

