package com.pfe.DFinancialStatement.financial_statement.mapper;

import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import org.springframework.stereotype.Component;

@Component
public class FinancialStatementMapper {

    public FinancialStatement toEntity(FinancialStatementDTO dto) {
        FinancialStatement entity = new FinancialStatement();
        entity.setFormData(dto.getFormData());
        entity.setReport(dto.getReport());  // Changed from setReportData to setReport
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }

    public FinancialStatementDTO toDTO(FinancialStatement entity) {
        FinancialStatementDTO dto = new FinancialStatementDTO();
        dto.setFormData(entity.getFormData());
        dto.setReport(entity.getReport());  // Changed from setReportData to setReport
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
