package com.pfe.DFinancialStatement.dmn_rule.service;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DmnExecutionService {

    private static final Set<String> CALCULATED_FIELDS = Set.of(
            "total_actif",
            "total_passif",
            "liquidites",
            "fonds_de_roulement",
            "bfr",
            "ratio_endettement",
            "investissements",
            "ressources_permanentes",
            "calculs"
    );

    private final DmnRuleRepository dmnRuleRepository;
    private final DmnEngine dmnEngine;

    public DmnExecutionService(DmnRuleRepository dmnRuleRepository) {
        this.dmnRuleRepository = dmnRuleRepository;
        this.dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    }

    public List<DmnRule> getAllDmnRules() {
        return dmnRuleRepository.findAll();
    }

    public DmnRule importDmn(String ruleKey, MultipartFile file) throws IOException {
        if (dmnRuleRepository.findByRuleKey(ruleKey).isPresent()) {
            throw new CustomException("RULE_KEY_EXISTS");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        DmnRule dmnRule = new DmnRule();
        dmnRule.setRuleKey(ruleKey);
        dmnRule.setRuleContent(content);

        return dmnRuleRepository.save(dmnRule);
    }

    private Set<String> extractDmnInputs(String dmnContent) {
        Set<String> inputs = new HashSet<>();

        // Extract direct inputData fields
        Pattern inputDataPattern = Pattern.compile(
                "<inputData id=\"[^\"]*\" name=\"([^\"]*)\"",
                Pattern.DOTALL
        );
        Matcher inputDataMatcher = inputDataPattern.matcher(dmnContent);
        while (inputDataMatcher.find()) {
            String input = normalizeFieldName(inputDataMatcher.group(1));
            inputs.add(input);
        }

        // Exclude BKM calculated fields
        Pattern bkmPattern = Pattern.compile(
                "<businessKnowledgeModel[^>]*>\\s*<variable name=\"([^\"]*)\"",
                Pattern.DOTALL
        );
        Matcher bkmMatcher = bkmPattern.matcher(dmnContent);
        if (bkmMatcher.find()) {
            inputs.remove(normalizeFieldName(bkmMatcher.group(1)));
        }

        return inputs;
    }

    private String normalizeFieldName(String rawName) {
        return rawName.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
    }

    public List<DmnRule> findCompatibleDmns(Set<String> formFields) {
        Set<String> normalizedFields = formFields.stream()
                .map(this::normalizeFieldName)
                .collect(Collectors.toSet());

        List<DmnRule> allDmns = dmnRuleRepository.findAll();
        List<DmnRule> compatible = new ArrayList<>();

        for (DmnRule rule : allDmns) {
            Set<String> dmnInputs = extractDmnInputs(rule.getRuleContent());
            Set<String> requiredInputs = dmnInputs.stream()
                    .filter(input -> !isCalculatedField(input))
                    .collect(Collectors.toSet());

            logDebugInfo(rule, dmnInputs, requiredInputs, normalizedFields);

            if (normalizedFields.containsAll(requiredInputs)) {
                compatible.add(rule);
            }
        }
        return compatible;
    }

    private boolean isCalculatedField(String input) {
        return CALCULATED_FIELDS.contains(input) || input.endsWith("_calculs");
    }

    private void logDebugInfo(DmnRule rule, Set<String> dmnInputs, Set<String> requiredInputs, Set<String> formFields) {
        System.out.printf("Checking DMN Rule %d:%n", rule.getId());
        System.out.println("Raw DMN Inputs: " + dmnInputs);
        System.out.println("Required Inputs: " + requiredInputs);
        System.out.println("Form Fields: " + formFields);

        Set<String> missing = requiredInputs.stream()
                .filter(f -> !formFields.contains(f))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            System.out.printf("Missing fields for Rule %d: %s%n", rule.getId(), missing);
        }

        System.out.println("----------------------------------------");
    }
}