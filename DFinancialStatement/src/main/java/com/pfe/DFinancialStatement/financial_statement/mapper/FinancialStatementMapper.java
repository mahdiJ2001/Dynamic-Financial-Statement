package com.pfe.DFinancialStatement.financial_statement.mapper;

import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.entity.StatementStatus;
import org.springframework.stereotype.Component;

@Component
public class FinancialStatementMapper {

    private final FinancialStatementMessageMapper messageMapper;

    public FinancialStatementMapper(FinancialStatementMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public FinancialStatement toEntity(FinancialStatementDTO dto) {
        FinancialStatement entity = new FinancialStatement();
        entity.setId(dto.getId());
        entity.setFormData(dto.getFormData());
        entity.setReport(dto.getReport());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : StatementStatus.PENDING);
        entity.setCompanyName(dto.getCompanyName());
        return entity;
    }

    public FinancialStatementDTO toDTO(FinancialStatement entity) {
        FinancialStatementDTO dto = new FinancialStatementDTO();
        dto.setId(entity.getId());
        dto.setFormData(entity.getFormData());
        dto.setReport(entity.getReport());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setStatus(entity.getStatus());
        dto.setCompanyName(entity.getCompanyName());
        if (entity.getCreatedBy() != null) {
            dto.setContributorName(entity.getCreatedBy().getUsername());
        }
        if (entity.getMessages() != null) {
            dto.setMessages(entity.getMessages().stream()
                    .map(messageMapper::toDTO)
                    .toList());
        }
        return dto;
    }
}


