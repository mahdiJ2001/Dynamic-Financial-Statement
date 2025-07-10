package com.pfe.DFinancialStatement.error_messages.controller;

import com.pfe.DFinancialStatement.error_messages.service.ErrorMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/errors")
public class ErrorMessageController {
    @Autowired
    private ErrorMessageService errorMessageService;

    @GetMapping("/{errorCode}")
    public ResponseEntity<Map<String, String>> getErrorMessage(@PathVariable String errorCode) {
        String message = errorMessageService.getErrorMessage(errorCode);
        Map<String, String> response = new HashMap<>();
        response.put("mappedError", message);
        return ResponseEntity.ok(response);
    }
}
