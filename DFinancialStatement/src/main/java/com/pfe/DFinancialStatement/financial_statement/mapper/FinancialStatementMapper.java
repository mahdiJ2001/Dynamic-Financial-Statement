package com.pfe.DFinancialStatement.financial_statement.mapper;

import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import org.springframework.stereotype.Component;

@Component
public class FinancialStatementMapper {

    public FinancialStatement toEntity(FinancialStatementDTO dto) {
        FinancialStatement entity = new FinancialStatement();
        entity.setFormData(dto.getFormData());
        entity.setAnalysisResult(dto.getAnalysisResult());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }

    public FinancialStatementDTO toDTO(FinancialStatement entity) {
        FinancialStatementDTO dto = new FinancialStatementDTO();
        dto.setFormData(entity.getFormData());
        dto.setAnalysisResult(entity.getAnalysisResult());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
