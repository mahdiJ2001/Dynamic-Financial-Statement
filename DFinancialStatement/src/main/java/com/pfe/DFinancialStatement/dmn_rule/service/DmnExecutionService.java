package com.pfe.DFinancialStatement.dmn_rule.service;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class DmnExecutionService {

    private final DmnRuleRepository dmnRuleRepository;
    private final DmnEngine dmnEngine;

    public DmnExecutionService(DmnRuleRepository dmnRuleRepository) {
        this.dmnRuleRepository = dmnRuleRepository;
        this.dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    }

    public String evaluateRisk(String ruleKey, double ratioEndettement, double ratioLiquidite, double ratioSolvabilite) {
        DmnRule rule = dmnRuleRepository.findByRuleKey(ruleKey)
                .orElse(null);

        if (rule == null) {
            return "Aucune règle trouvée pour la clé : " + ruleKey;
        }

        String dmnXml = rule.getRuleContent();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dmnXml.getBytes(StandardCharsets.UTF_8));

        DmnDecision decision = dmnEngine.parseDecision(ruleKey, inputStream);
        VariableMap variables = Variables.createVariables()
                .putValue("ratioEndettement", ratioEndettement)
                .putValue("ratioLiquiditeGenerale", ratioLiquidite)
                .putValue("ratioSolvabilite", ratioSolvabilite);

        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);

        return result.isEmpty() ? "Non classé" : result.getSingleResult().get("Risk").toString();
    }

}
