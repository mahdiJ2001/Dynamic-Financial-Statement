package com.pfe.DFinancialStatement.financial_statement.mapper;

import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementMessageDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatementMessage;
import org.springframework.stereotype.Component;

@Component
public class FinancialStatementMessageMapper {

    public FinancialStatementMessageDTO toDTO(FinancialStatementMessage entity) {
        FinancialStatementMessageDTO dto = new FinancialStatementMessageDTO();
        dto.setId(entity.getId());
        dto.setMessageContent(entity.getMessageContent());
        dto.setSentAt(entity.getSentAt());
        dto.setSenderUsername(entity.getSender().getUsername());
        return dto;
    }
}
