package com.pfe.DFinancialStatement.financial_statement.controller;

import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.service.FinancialStatementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/financial-statements")
public class FinancialStatementController {

    @Autowired
    private FinancialStatementService financialStatementService;

    @PostMapping
    public ResponseEntity<FinancialStatementDTO> saveFinancialStatement(@RequestBody FinancialStatementDTO dto) {
        FinancialStatementDTO savedStatement = financialStatementService.saveFinancialStatement(dto);
        return ResponseEntity.ok(savedStatement);
    }

    @GetMapping
    public ResponseEntity<List<FinancialStatementDTO>> getAllFinancialStatements() {
        List<FinancialStatementDTO> statements = financialStatementService.getAllFinancialStatements();
        return ResponseEntity.ok(statements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancialStatementDTO> getFinancialStatementById(@PathVariable Long id) {
        Optional<FinancialStatementDTO> statement = financialStatementService.getFinancialStatementById(id);
        return statement.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
