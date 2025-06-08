package com.pfe.DFinancialStatement.financial_statement.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FinancialStatementMessageDTO {
    private Long id;
    private String messageContent;
    private String senderUsername;
    private LocalDateTime sentAt;
}
