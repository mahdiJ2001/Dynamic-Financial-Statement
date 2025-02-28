package com.pfe.DFinancialStatement.financial_statement.service;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.mapper.FinancialStatementMapper;
import com.pfe.DFinancialStatement.financial_statement.repository.FinancialStatementRepository;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FinancialStatementService {

    @Autowired
    private FinancialStatementRepository financialStatementRepository;

    @Autowired
    private FinancialStatementMapper financialStatementMapper;

    @Autowired
    private DmnRuleRepository dmnRuleRepository; // Repository to retrieve DMN rules from Postgres

    public FinancialStatementDTO saveFinancialStatement(FinancialStatementDTO dto) {
        // Apply DMN rules and calculate the financial indicators
        String analysisResult = applyDmnRules(dto.getFormData());

        // Convert DTO to entity and save
        FinancialStatement entity = financialStatementMapper.toEntity(dto);
        entity.setAnalysisResult(analysisResult);
        entity = financialStatementRepository.save(entity);

        return financialStatementMapper.toDTO(entity);
    }

    private String applyDmnRules(String formData) {
        // Parse the formData JSON string into a map or object
        JSONObject formJson = new JSONObject(formData);

        // Extract values for assets (actif) and liabilities (passif)
        JSONObject actif = formJson.getJSONObject("actif");
        JSONObject passif = formJson.getJSONObject("passif");

        // Calcul du total de l'actif
        double totalActif = 0;
        for (String key : actif.keySet()) {
            JSONObject fields = actif.getJSONObject(key);
            for (String fieldKey : fields.keySet()) {
                totalActif += fields.getDouble(fieldKey);
            }
        }

        // Calcul du total du passif
        double totalPassif = 0;
        for (String key : passif.keySet()) {
            JSONObject fields = passif.getJSONObject(key);
            for (String fieldKey : fields.keySet()) {
                totalPassif += fields.getDouble(fieldKey);
            }
        }

        // Calcul des fonds propres
        double fondsPropres = totalActif - totalPassif;

        // Calcul du ratio d'endettement
        double totalDettes = 0;
        if (passif.has("Dettes")) {
            JSONObject dettes = passif.getJSONObject("Dettes");
            totalDettes += dettes.getDouble("Dettes à court terme");
            totalDettes += dettes.getDouble("Dettes à long terme");
        }
        double ratioEndettement = totalDettes / fondsPropres;

        // Calcul du ratio de liquidité générale
        double actifsCourtTerme = 0;
        if (actif.has("Créances clients")) {
            actifsCourtTerme += actif.getJSONObject("Créances clients").getDouble("value");
        }
        if (actif.has("Disponibilités")) {
            actifsCourtTerme += actif.getJSONObject("Disponibilités").getDouble("value");
        }
        double dettesCourtTerme = passif.has("Dettes") ? passif.getJSONObject("Dettes").getDouble("Dettes à court terme") : 0;
        double ratioLiquiditeGenerale = actifsCourtTerme / dettesCourtTerme;

        // Calcul du ratio de solvabilité
        double ratioSolvabilite = totalActif / totalDettes;

        // Construct the result as a JSON object
        JSONObject result = new JSONObject();
        result.put("totalActif", totalActif);
        result.put("totalPassif", totalPassif);
        result.put("fondsPropres", fondsPropres);
        result.put("ratioEndettement", ratioEndettement);
        result.put("ratioLiquiditeGenerale", ratioLiquiditeGenerale);
        result.put("ratioSolvabilite", ratioSolvabilite);

        // Return the result as a string (can be converted to JSON)
        return result.toString();
    }

    public List<FinancialStatementDTO> getAllFinancialStatements() {
        List<FinancialStatement> entities = financialStatementRepository.findAll();
        return entities.stream().map(financialStatementMapper::toDTO).toList();
    }

    public Optional<FinancialStatementDTO> getFinancialStatementById(Long id) {
        return financialStatementRepository.findById(id).map(financialStatementMapper::toDTO);
    }
}
