package com.pfe.DFinancialStatement.dmn_rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.dmn_rule.dto.ExpressionEvaluationResult;
import com.pfe.DFinancialStatement.dmn_rule.dto.RuleDto;
import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DmnEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(DmnEvaluationService.class);

    @Autowired
    private DmnRuleRepository dmnRuleRepository;

    private final DmnEngine dmnEngine;
    private final ObjectMapper objectMapper;
    private final org.camunda.feel.FeelEngine feelEngine;

    public DmnEvaluationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.dmnEngine = DmnEngineConfiguration
                .createDefaultDmnEngineConfiguration()
                .buildEngine();
        this.feelEngine = new org.camunda.feel.FeelEngine.Builder().build();
    }

    public List<ExpressionEvaluationResult> evaluateDmn(
            String ruleKey,
            List<RuleDto> allRules,
            Map<String, Object> inputData
    ) throws JsonProcessingException {
        DmnRule dmnRule = dmnRuleRepository.findByRuleKey(ruleKey)
                .orElseThrow(() -> new RuntimeException("DMN rule not found for key: " + ruleKey));

        String dmnContent = dmnRule.getRuleContent();

        // Parse DMN decision
        DmnDecision decision = dmnEngine.parseDecision(
                "Validation_Bilan",
                new ByteArrayInputStream(dmnContent.getBytes(StandardCharsets.UTF_8))
        );

        // Extract input expressions
        DmnModelInstance dmnModel = Dmn.readModelFromStream(
                new ByteArrayInputStream(dmnContent.getBytes(StandardCharsets.UTF_8))
        );

        DecisionTable decisionTable = dmnModel.getModelElementsByType(DecisionTable.class)
                .iterator()
                .next();

        List<String> inputExpressions = decisionTable.getInputs().stream()
                .map(input -> input.getInputExpression().getText().getTextContent())
                .collect(Collectors.toList());

        // Evaluate expressions
        Map<String, Double> expressionToValue = new HashMap<>();
        for (String expr : inputExpressions) {
            try {
                // Evaluate expression
                Object result = feelEngine.evalExpression(expr, inputData);

                System.out.println("Evaluated result");
                System.out.println(result);

                Double value = extractNumericValue(result);

                System.out.println("Evaluated value");
                System.out.println(value);

                expressionToValue.put(expr, value);
                logger.debug("Evaluated expression '{}' = {}", expr, value);
            } catch (Exception e) {
                logger.warn("Failed to evaluate expression '{}': {}", expr, e.getMessage());
                expressionToValue.put(expr, null);
            }
        }

        // Evaluate DMN rules
        DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, inputData);

        Map<String, Map<String, Object>> matchedRuleMap = decisionResult.getResultList().stream()
                .filter(entry -> entry.containsKey("messageErreur"))
                .collect(Collectors.toMap(
                        entry -> (String) entry.get("messageErreur"),
                        entry -> entry
                ));

        // Build results
        List<ExpressionEvaluationResult> evaluationResults = new ArrayList<>();
        for (RuleDto rule : allRules) {
            Map<String, Object> matchedRow = matchedRuleMap.get(rule.getMessageErreur());
            boolean matched = matchedRow != null;

            // Match expressions with proper string handling
            String normalizedExpression = rule.getExpression().replaceAll("\\s+", " ").trim();
            Double evaluatedValue = expressionToValue.entrySet().stream()
                    .filter(e -> e.getKey() != null && normalizedExpression.equalsIgnoreCase(
                            e.getKey().replaceAll("\\s+", " ").trim()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

            evaluationResults.add(new ExpressionEvaluationResult(
                    rule.getExpression(),
                    rule.getCondition() + rule.getValue(),
                    matched,
                    rule.getMessageErreur(),
                    rule.getSeverite(),
                    evaluatedValue
            ));
        }

        logger.debug("DMN Evaluation Results: {}", objectMapper.writeValueAsString(evaluationResults));
        return evaluationResults;
    }

    private Double extractNumericValue(Object result) {
        if (result == null) {
            return null;
        }

        // Handle FEEL engine Right() success case - Map structure
        if (result instanceof Map) {
            Map<?, ?> resultMap = (Map<?, ?>) result;
            if (resultMap.containsKey("Right")) {
                Object rightValue = resultMap.get("Right");
                // Handle numeric value inside Right
                if (rightValue instanceof Number) {
                    return ((Number) rightValue).doubleValue();
                }
                // Handle string value inside Right that could be a number
                else if (rightValue instanceof String) {
                    try {
                        return Double.parseDouble((String) rightValue);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                // Handle null inside Right
                else if (rightValue == null) {
                    return null;
                }
            }
        }

        // Handle direct numeric value
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }

        // Handle string representation (fallback)
        if (result instanceof String) {
            String strResult = (String) result;
            // Check for Right wrapper in string format
            if (strResult.startsWith("Right(") && strResult.endsWith(")")) {
                try {
                    String numericPart = strResult.substring(6, strResult.length() - 1);
                    if (numericPart.equals("null")) {
                        return null;
                    }
                    return Double.parseDouble(numericPart);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                // Handle plain numeric string
                try {
                    return Double.parseDouble(strResult);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }
}