package com.pfe.DFinancialStatement.financial_statement.controller;

import com.pfe.DFinancialStatement.dmn_rule.dto.ExpressionEvaluationResult;
import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.StatementStatus;
import com.pfe.DFinancialStatement.financial_statement.service.FinancialStatementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/financial-statements")
public class FinancialStatementController {

    @Autowired
    private FinancialStatementService financialStatementService;

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

    @PostMapping
    public ResponseEntity<List<ExpressionEvaluationResult>> evaluateAndSaveFinancialStatement(
            @RequestBody FinancialStatementDTO financialStatementDTO,
            @RequestParam String ruleKey,
            @RequestParam String designName) {

        List<ExpressionEvaluationResult> evaluationResults =
                financialStatementService.evaluateAndSaveStatement(financialStatementDTO, ruleKey, designName);

        return ResponseEntity.ok(evaluationResults);
    }


    @PutMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String rejectionCause) {
        Map<String, Object> result = financialStatementService.updateStatus(id, status, rejectionCause);
        return ResponseEntity.ok(result);
    }

}
