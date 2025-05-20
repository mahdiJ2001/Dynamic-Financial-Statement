package com.pfe.DFinancialStatement.dmn_rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.dmn_rule.dto.ExpressionEvaluationResult;
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

    public List<ExpressionEvaluationResult> evaluateDmn(String ruleKey, Map<String, Object> inputData) throws JsonProcessingException {
        DmnRule dmnRule = dmnRuleRepository.findByRuleKey(ruleKey)
                .orElseThrow();

        String dmnContent = dmnRule.getRuleContent();

        DmnDecision decision = dmnEngine.parseDecision("Validation_Bilan",
                new ByteArrayInputStream(dmnContent.getBytes(StandardCharsets.UTF_8)));

        DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, inputData);

        List<ExpressionEvaluationResult> evaluationResults = new ArrayList<>();

        decisionResult.getResultList().forEach(result -> {
            String messageErreur = (String) result.get("messageErreur");
            String severite = (String) result.get("severite");

            evaluationResults.add(new ExpressionEvaluationResult(
                    null,
                    null,
                    true,
                    messageErreur,
                    severite
            ));
        });

        logger.info("DMN Engine Evaluation Result: {}", objectMapper.writeValueAsString(evaluationResults));
        return evaluationResults;
    }

}
