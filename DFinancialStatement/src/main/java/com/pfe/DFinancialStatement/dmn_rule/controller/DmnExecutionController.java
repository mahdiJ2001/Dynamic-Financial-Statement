package com.pfe.DFinancialStatement.dmn_rule.controller;

import com.pfe.DFinancialStatement.dmn_rule.dto.RuleDto;
import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnRuleImportService;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnRuleAICompatibilityService;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnRuleStaticCompatibilityService;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import com.pfe.DFinancialStatement.error_messages.service.ErrorMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnXmlGenerationService;

import java.util.*;

@RestController
public class DmnExecutionController {

    private final DmnRuleImportService dmnRuleImportService;
    private final DmnRuleAICompatibilityService dmnRuleAICompatibilityService;
    private final DmnRuleStaticCompatibilityService dmnRuleStaticCompatibilityService;
    private final DmnXmlGenerationService dmnXmlGenerationService;

    private final ErrorMessageService errorMessageService;


    @Autowired
    public DmnExecutionController(DmnRuleImportService dmnRuleImportService,
                                  DmnRuleAICompatibilityService dmnRuleAICompatibilityService,
                                  DmnRuleStaticCompatibilityService dmnRuleStaticCompatibilityService, DmnXmlGenerationService dmnXmlGenerationService, ErrorMessageService errorMessageService) {
        this.dmnRuleImportService = dmnRuleImportService;
        this.dmnRuleAICompatibilityService = dmnRuleAICompatibilityService;
        this.dmnRuleStaticCompatibilityService = dmnRuleStaticCompatibilityService;
        this.dmnXmlGenerationService = dmnXmlGenerationService;
        this.errorMessageService = errorMessageService;
    }

    @GetMapping("/dmn")
    public ResponseEntity<List<DmnRule>> getAllDmnRules() {
        List<DmnRule> dmnRules = dmnRuleImportService.getAllDmnRules();
        return ResponseEntity.ok(dmnRules);
    }


    /*
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
        } catch (CustomException e) {
            String errorMessage = errorMessageService.getErrorMessage(e.getErrorCode());

            Map<String, String> response = new HashMap<>();
            response.put("error", errorMessage);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }*/

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

    @PostMapping("/dmn/create")
    public ResponseEntity<?> createDmnRule(
            @RequestParam("ruleKey") String ruleKey,
            @RequestBody List<RuleDto> rules) {

        String xmlContent = dmnXmlGenerationService.generateDmnXmlFromRuleDtoList(rules);
        dmnRuleImportService.saveNewDmnRule(ruleKey, xmlContent);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Collections.singletonMap("message", "Règle DMN créée et sauvegardée avec succès."));
    }

}
