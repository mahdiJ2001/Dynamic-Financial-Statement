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
        // 1) Charger le DMN
        DmnRule dmnRule = dmnRuleRepository.findByRuleKey(ruleKey)
                .orElseThrow();
        String dmnContent = dmnRule.getRuleContent();
        DmnDecision decision = dmnEngine.parseDecision(
                "Validation_Bilan",
                new ByteArrayInputStream(dmnContent.getBytes(StandardCharsets.UTF_8))
        );
        DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, inputData);

        // 2) Extraire les messages d'erreur des règles matchées
        Set<String> matchedMessages = decisionResult.getResultList().stream()
                .map(m -> (String) m.get("messageErreur"))
                .collect(Collectors.toSet());

        // 3) Construire le résultat complet
        List<ExpressionEvaluationResult> evaluationResults = new ArrayList<>();
        for (RuleDto rule : allRules) {
            boolean matched = matchedMessages.contains(rule.getMessageErreur());
            evaluationResults.add(new ExpressionEvaluationResult(
                    rule.getExpression(),
                    rule.getCondition() + rule.getValue(),
                    matched,
                    rule.getMessageErreur(),
                    rule.getSeverite()
            ));
        }

        logger.info("DMN Full Evaluation Result: {}",
                objectMapper.writeValueAsString(evaluationResults));
        return evaluationResults;
    }


}
