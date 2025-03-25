package com.pfe.DFinancialStatement.error_messages.handler;

import com.pfe.DFinancialStatement.error_messages.service.ErrorMessageService;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorMessageService errorMessageService;

    public GlobalExceptionHandler(ErrorMessageService errorMessageService) {
        this.errorMessageService = errorMessageService;
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, String>> handleCustomException(CustomException ex) {
        String errorMessage = errorMessageService.getErrorMessage(ex.getErrorCode());

        Map<String, String> response = new HashMap<>();
        response.put("errorCode", ex.getErrorCode());
        response.put("mappedError", errorMessage);

        System.out.println("Message d'erreur retourné : " + errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
