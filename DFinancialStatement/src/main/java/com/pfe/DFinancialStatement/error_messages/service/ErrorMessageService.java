package com.pfe.DFinancialStatement.error_messages.service;

import com.pfe.DFinancialStatement.error_messages.repository.ErrorMessageRepository;
import com.pfe.DFinancialStatement.error_messages.entity.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ErrorMessageService {
    @Autowired
    private ErrorMessageRepository errorMessageRepository;

    public String getErrorMessage(String errorCode) {
        return errorMessageRepository.findByErrorCode(errorCode)
                .map(ErrorMessage::getCustomMessage)
                .orElse("Unknown error occurred");
    }
}

