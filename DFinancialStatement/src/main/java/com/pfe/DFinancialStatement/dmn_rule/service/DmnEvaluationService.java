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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DmnEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(DmnEvaluationService.class);

    @Autowired
    private DmnRuleRepository dmnRuleRepository;

    private final DmnEngine dmnEngine;
    private final ObjectMapper objectMapper;

    public DmnEvaluationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    }

    public List<ExpressionEvaluationResult> evaluateDmn(
            String ruleKey,
            List<RuleDto> allRules,
            Map<String, Object> inputData
    ) throws JsonProcessingException {

        DmnRule dmnRule = dmnRuleRepository.findByRuleKey(ruleKey)
                .orElseThrow();
        String dmnContent = dmnRule.getRuleContent();
        DmnDecision decision = dmnEngine.parseDecision(
                "Validation_Bilan",
                new ByteArrayInputStream(dmnContent.getBytes(StandardCharsets.UTF_8))
        );

        DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, inputData);

        // On crée un map : messageErreur → ligne de résultat complète
        Map<String, Map<String, Object>> matchedRuleMap = decisionResult.getResultList().stream()
                .filter(entry -> entry.containsKey("messageErreur"))
                .collect(Collectors.toMap(
                        entry -> (String) entry.get("messageErreur"),
                        entry -> entry
                ));

        List<ExpressionEvaluationResult> evaluationResults = new ArrayList<>();
        for (RuleDto rule : allRules) {
            Map<String, Object> matchedRow = matchedRuleMap.get(rule.getMessageErreur());
            boolean matched = matchedRow != null;

            Double evaluatedValue = matched ? (Double) matchedRow.get("evaluatedValue") : null;

            evaluationResults.add(new ExpressionEvaluationResult(
                    rule.getExpression(),
                    rule.getCondition() + rule.getValue(),
                    matched,
                    rule.getMessageErreur(),
                    rule.getSeverite(),
                    evaluatedValue
            ));
        }

        logger.info("DMN Full Evaluation Result: {}",
                objectMapper.writeValueAsString(evaluationResults));
        return evaluationResults;
    }



}
