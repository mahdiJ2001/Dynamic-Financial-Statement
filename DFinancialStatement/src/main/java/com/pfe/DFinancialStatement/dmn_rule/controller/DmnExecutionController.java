package com.pfe.DFinancialStatement.dmn_rule.controller;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnRuleImportService;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnRuleAICompatibilityService;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnRuleStaticCompatibilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
public class DmnExecutionController {

    private final DmnRuleImportService dmnRuleImportService;
    private final DmnRuleAICompatibilityService dmnRuleAICompatibilityService;
    private final DmnRuleStaticCompatibilityService dmnRuleStaticCompatibilityService;

    @Autowired
    public DmnExecutionController(DmnRuleImportService dmnRuleImportService,
                                  DmnRuleAICompatibilityService dmnRuleAICompatibilityService,
                                  DmnRuleStaticCompatibilityService dmnRuleStaticCompatibilityService) {
        this.dmnRuleImportService = dmnRuleImportService;
        this.dmnRuleAICompatibilityService = dmnRuleAICompatibilityService;
        this.dmnRuleStaticCompatibilityService = dmnRuleStaticCompatibilityService;
    }

    @GetMapping("/dmn")
    public ResponseEntity<List<DmnRule>> getAllDmnRules() {
        List<DmnRule> dmnRules = dmnRuleImportService.getAllDmnRules();
        return ResponseEntity.ok(dmnRules);
    }

    @PostMapping("/dmn/import")
    public ResponseEntity<?> importDmn(
            @RequestParam("ruleKey") String ruleKey,
            @RequestParam("file") MultipartFile file) {
        try {
            DmnRule rule = dmnRuleImportService.importDmn(ruleKey, file);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "DMN importé avec succès");
            response.put("ruleKey", rule.getRuleKey());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de l'import du DMN");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    
    @GetMapping("/dmn/compatible/ai")
    public ResponseEntity<List<DmnRule>> getCompatibleDmnsAI(@RequestParam("fields") String fields) {
        Set<String> formFields = new HashSet<>(Arrays.asList(fields.split(",")));
        List<DmnRule> compatibleDmns = dmnRuleAICompatibilityService.findCompatibleDmnsWithAI(formFields);
        return ResponseEntity.ok(compatibleDmns);
    }


    @GetMapping("/dmn/compatible/static")
    public ResponseEntity<List<DmnRule>> getCompatibleDmnsStatic(@RequestParam("fields") String fields) {
        Set<String> formFields = new HashSet<>(Arrays.asList(fields.split(",")));
        List<DmnRule> compatibleDmns = dmnRuleStaticCompatibilityService.findCompatibleDmns(formFields);
        return ResponseEntity.ok(compatibleDmns);
    }
}
