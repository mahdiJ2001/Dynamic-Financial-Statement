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
import java.text.Normalizer;
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

        // Create a mapping between normalized expressions (from DMN) and original expressions (from RuleDto)
        Map<String, String> normalizedToOriginalMap = new HashMap<>();
        for (RuleDto rule : allRules) {
            String originalExpr = rule.getExpression();
            String normalizedExpr = normalizeExpression(originalExpr);
            normalizedToOriginalMap.put(normalizedExpr, originalExpr);
        }

        // Evaluate expressions
        Map<String, Double> expressionToValue = new HashMap<>();
        for (String expr : inputExpressions) {
            try {
                // Evaluate expression
                Object result = feelEngine.evalExpression(expr, inputData);

                System.out.println("Evaluated result for expression: " + expr);
                System.out.println(result);

                // Debug output to see actual type and structure
                if (result != null) {
                    System.out.println("Result class: " + result.getClass().getName());
                    if (result instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) result;
                        System.out.println("Map keys: " + map.keySet());
                        for (Object key : map.keySet()) {
                            Object value = map.get(key);
                            System.out.println("Key: " + key + ", Value: " + value +
                                    (value != null ? ", Value class: " + value.getClass().getName() : ", Value class: null"));
                        }
                    }
                }

                Double value = extractNumericValue(result);

                System.out.println("Evaluated value for expression: " + expr + " = " + value);

                // Store the value using the normalized expression as key
                expressionToValue.put(expr, value);

                // Also store using the original expression if we can find a mapping
                String originalExpr = normalizedToOriginalMap.get(expr);
                if (originalExpr != null) {
                    expressionToValue.put(originalExpr, value);
                    System.out.println("Mapped normalized expression '" + expr + "' to original expression '" + originalExpr + "' with value: " + value);
                }

            } catch (Exception e) {
                logger.warn("Failed to evaluate expression '{}': {}", expr, e.getMessage());
                expressionToValue.put(expr, null);

                // Also store null for original expression if mapping exists
                String originalExpr = normalizedToOriginalMap.get(expr);
                if (originalExpr != null) {
                    expressionToValue.put(originalExpr, null);
                }
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


            Double evaluatedValue = expressionToValue.get(rule.getExpression());


            if (evaluatedValue == null) {
                String normalizedExpr = normalizeExpression(rule.getExpression());
                evaluatedValue = expressionToValue.get(normalizedExpr);
            }

            System.out.println("For rule expression '" + rule.getExpression() + "', found evaluated value: " + evaluatedValue);

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

        // Special case for Scala Right wrapper which might be returned by FEEL engine
        if (result.toString().startsWith("Right(") && result.toString().endsWith(")")) {
            String content = result.toString().substring(6, result.toString().length() - 1);
            if (content.equals("null")) {
                return null;
            }
            try {
                return Double.parseDouble(content);
            } catch (NumberFormatException e) {
                logger.debug("Could not parse numeric value from Right wrapper: {}", content);
                return null;
            }
        }

        // Handle Map containing Right key (FEEL engine's successful evaluation)
        if (result instanceof Map) {
            Map<?, ?> resultMap = (Map<?, ?>) result;
            if (resultMap.containsKey("Right")) {
                Object rightValue = resultMap.get("Right");
                if (rightValue == null) {
                    return null;
                }
                // If the right value is a number
                if (rightValue instanceof Number) {
                    return ((Number) rightValue).doubleValue();
                }
                // If the right value is a string that can be parsed as a number
                if (rightValue instanceof String) {
                    try {
                        return Double.parseDouble((String) rightValue);
                    } catch (NumberFormatException e) {
                        logger.debug("Could not parse string to double: {}", rightValue);
                        return null;
                    }
                }
                // If the right value is something else, try using toString and parsing
                try {
                    return Double.parseDouble(rightValue.toString());
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse object to double: {}", rightValue);
                    return null;
                }
            }
        }

        // Direct numeric value
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }

        // Try parsing as string directly
        try {
            return Double.parseDouble(result.toString());
        } catch (NumberFormatException e) {
            logger.debug("Could not parse to double: {}", result);
            return null;
        }
    }

    /**
     * Normalize expression by replacing spaces in variable names with underscores
     * This should match the normalization logic used in DmnXmlGenerationService
     */
    private String normalizeExpression(String input) {
        if (input == null) return "";

        // Supprimer les accents
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("œ", "oe")
                .replaceAll("æ", "ae");

        // Séparer les opérateurs et parenthèses pour traiter les noms
        String[] tokens = normalized.split("(?=[-+*/<>=()])|(?<=[-+*/<>=()])");

        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            String trimmed = token.trim();

            // Si le token est un nom composé avec des espaces : remplace les espaces par _
            if (trimmed.matches("[\\p{L}0-9_]+(\\s+[\\p{L}0-9_]+)+")) {
                result.append(trimmed.replaceAll("\\s+", "_"));
            } else {
                result.append(trimmed);
            }
        }

        return result.toString();
    }
}