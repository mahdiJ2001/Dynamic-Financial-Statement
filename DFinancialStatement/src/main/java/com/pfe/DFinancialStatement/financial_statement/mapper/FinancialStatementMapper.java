package com.pfe.DFinancialStatement.financial_statement.mapper;

import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import org.springframework.stereotype.Component;

@Component
public class FinancialStatementMapper {

    public FinancialStatement toEntity(FinancialStatementDTO dto) {
        FinancialStatement entity = new FinancialStatement();
        entity.setFormData(dto.getFormData());
        entity.setReport(dto.getReport());
        entity.setCreatedAt(dto.getCreatedAt());
        // createdBy non mappé ici, car on suppose que c'est défini côté service
        return entity;
    }

    public FinancialStatementDTO toDTO(FinancialStatement entity) {
        FinancialStatementDTO dto = new FinancialStatementDTO();
        dto.setFormData(entity.getFormData());
        dto.setReport(entity.getReport());
        dto.setCreatedAt(entity.getCreatedAt());
        if (entity.getCreatedBy() != null) {
            dto.setContributorName(entity.getCreatedBy().getUsername()); // 👈 Set contributor
        }
        return dto;
    }
}
