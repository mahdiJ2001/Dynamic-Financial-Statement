package com.pfe.DFinancialStatement.dmn_rule.controller;

import com.pfe.DFinancialStatement.dmn_rule.service.DmnExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DmnExecutionController {

    private final DmnExecutionService dmnExecutionService;

    @Autowired
    public DmnExecutionController(DmnExecutionService dmnExecutionService) {
        this.dmnExecutionService = dmnExecutionService;
    }

    @GetMapping("/evaluateRisk")
    public String evaluateRisk(
            @RequestParam String ruleKey,
            @RequestParam double ratioEndettement,
            @RequestParam double ratioLiquidite,
            @RequestParam double ratioSolvabilite) {

        return dmnExecutionService.evaluateRisk(ruleKey, ratioEndettement, ratioLiquidite, ratioSolvabilite);
    }
}
