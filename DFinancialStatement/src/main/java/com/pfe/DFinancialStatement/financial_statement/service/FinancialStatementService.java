package com.pfe.DFinancialStatement.financial_statement.service;

import com.pfe.DFinancialStatement.dmn_rule.service.DmnExecutionService;
import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.mapper.FinancialStatementMapper;
import com.pfe.DFinancialStatement.financial_statement.repository.FinancialStatementRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FinancialStatementService {

    @Autowired
    private FinancialStatementRepository financialStatementRepository;

    @Autowired
    private FinancialStatementMapper financialStatementMapper;

    @Autowired
    private DmnExecutionService dmnExecutionService;

    public String saveFinancialStatement(FinancialStatementDTO dto) {
        String analysisResult = applyDmnRules(dto.getFormData());

        FinancialStatement entity = financialStatementMapper.toEntity(dto);
        entity.setAnalysisResult(analysisResult);

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }

        financialStatementRepository.save(entity);

        return analysisResult;
    }


    private String applyDmnRules(String formData) {

        if (formData == null || formData.isEmpty()) {
            return "{\"error\": \"Invalid form data\"}";
        }

        try {

            JSONObject formJson = new JSONObject(formData);

            if (!formJson.has("formStructure")) {
                return "{\"error\": \"Missing 'formStructure' key\"}";
            }

            JSONObject formStructure = new JSONObject(formJson.getString("formStructure"));


            if (!formStructure.has("actif") || !formStructure.has("passif")) {
                return "{\"error\": \"Missing 'actif' or 'passif' data\"}";
            }

            JSONArray actif = formStructure.getJSONArray("actif");
            JSONArray passif = formStructure.getJSONArray("passif");

            double totalActif = calculateTotal(actif);
            double totalPassif = calculateTotal(passif);

            double ratioEndettement = calculateDebtRatio(totalActif, totalPassif);
            double ratioLiquiditeGenerale = calculateLiquidityRatio(actif, passif);
            double ratioSolvabilite = calculateSolvencyRatio(totalActif, totalPassif);

            // Use DMN service to evaluate risk with rule key "RiskAssessment"
            String risk = dmnExecutionService.evaluateRisk(
                    "RiskAssessment",
                    ratioEndettement,
                    ratioLiquiditeGenerale,
                    ratioSolvabilite
            );

            // Construct the result JSON
            JSONObject result = new JSONObject();
            result.put("totalActif", totalActif);
            result.put("totalPassif", totalPassif);
            result.put("ratioEndettement", ratioEndettement);
            result.put("ratioLiquiditeGenerale", ratioLiquiditeGenerale);
            result.put("ratioSolvabilite", ratioSolvabilite);
            result.put("risk", risk);

            return result.toString();

        } catch (Exception e) {
            JSONObject errorResult = new JSONObject();
            errorResult.put("error", "Error processing form data: " + e.getMessage());
            return errorResult.toString();
        }
    }

    private double calculateTotal(JSONArray items) {
        double total = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (!item.has("fields")) continue;

            JSONArray fields = item.getJSONArray("fields");
            for (int j = 0; j < fields.length(); j++) {
                JSONObject field = fields.getJSONObject(j);
                double value = field.has("value") ? field.getDouble("value") : 0;
                total += value;
            }
        }
        return total;
    }

    private double calculateDebtRatio(double totalActif, double totalPassif) {
        return totalActif == 0 ? 0 : totalPassif / totalActif;
    }

    private double calculateSolvencyRatio(double totalActif, double totalPassif) {
        return totalActif == 0 ? 0 : (totalActif - totalPassif) / totalActif;
    }

    private double calculateLiquidityRatio(JSONArray actif, JSONArray passif) {
        double actifsCourtTerme = 0;
        double dettesCourtTerme = 0;

        for (int i = 0; i < actif.length(); i++) {
            JSONObject item = actif.getJSONObject(i);
            if (!item.has("fields")) continue;

            if (item.getString("name").equals("Créances clients") ||
                    item.getString("name").equals("Disponibilités")) {
                JSONArray fields = item.getJSONArray("fields");
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject field = fields.getJSONObject(j);
                    double value = field.has("value") ? field.getDouble("value") : 0;
                    actifsCourtTerme += value;
                }
            }
        }

        for (int i = 0; i < passif.length(); i++) {
            JSONObject item = passif.getJSONObject(i);
            if (!item.has("fields")) continue;

            if (item.getString("name").equals("Dettes")) {
                JSONArray fields = item.getJSONArray("fields");
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject field = fields.getJSONObject(j);
                    if (field.getString("label").contains("court terme")) {
                        double value = field.has("value") ? field.getDouble("value") : 0;
                        dettesCourtTerme += value;
                    }
                }
            }
        }

        return dettesCourtTerme == 0 ? 0 : actifsCourtTerme / dettesCourtTerme;
    }

    public List<FinancialStatementDTO> getAllFinancialStatements() {
        List<FinancialStatement> entities = financialStatementRepository.findAll();
        return entities.stream().map(financialStatementMapper::toDTO).toList();
    }

    public Optional<FinancialStatementDTO> getFinancialStatementById(Long id) {
        return financialStatementRepository.findById(id).map(financialStatementMapper::toDTO);
    }
}
