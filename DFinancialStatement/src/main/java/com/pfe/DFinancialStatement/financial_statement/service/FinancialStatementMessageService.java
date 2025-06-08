package com.pfe.DFinancialStatement.financial_statement.service;

import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.UserService;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatementMessage;
import com.pfe.DFinancialStatement.financial_statement.repository.FinancialStatementMessageRepository;
import com.pfe.DFinancialStatement.financial_statement.repository.FinancialStatementRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinancialStatementMessageService {

    @Autowired
    private FinancialStatementMessageRepository messageRepository;

    @Autowired
    private FinancialStatementRepository financialStatementRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public List<FinancialStatementMessage> getMessagesByFinancialStatementId(Long financialStatementId) {
        return messageRepository.findByFinancialStatementIdOrderBySentAtAsc(financialStatementId);
    }

    @Transactional
    public FinancialStatementMessage addMessage(Long financialStatementId, Long senderId, String content) {
        FinancialStatement fs = financialStatementRepository.findById(financialStatementId)
                .orElseThrow(() -> new RuntimeException("FinancialStatement not found with id: " + financialStatementId));

        User sender = userService.getUserById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + senderId));

        FinancialStatementMessage message = FinancialStatementMessage.builder()
                .financialStatement(fs)
                .sender(sender)
                .messageContent(content)
                .sentAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }
}
