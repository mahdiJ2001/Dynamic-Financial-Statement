package com.pfe.DFinancialStatement.financial_statement.service;

import com.pfe.DFinancialStatement.dmn_rule.service.DmnEvaluationService;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.mapper.FinancialStatementMapper;
import com.pfe.DFinancialStatement.financial_statement.repository.FinancialStatementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FinancialStatementService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialStatementService.class);

    @Autowired
    private FinancialStatementRepository financialStatementRepository;

    @Autowired
    private FinancialStatementMapper financialStatementMapper;

    @Autowired
    private DmnEvaluationService dmnEvaluationService;

    @Autowired
    private ObjectMapper objectMapper; // For JSON parsing

    public String evaluateAndSaveStatement(FinancialStatementDTO dto, String ruleKey) {
        try {
            // Convert JSON string to Map
            Map<String, Object> rawData = objectMapper.readValue(dto.getFormData(), Map.class);
            Map<String, Object> inputData = new HashMap<>();

            extractFields(rawData, "actif", inputData);
            extractFields(rawData, "passif", inputData);

            // Evaluate DMN rule
            String dmnResultJson = dmnEvaluationService.evaluateDmn(ruleKey, inputData);

            // Parse decision result from DMN evaluation
            JSONObject jsonResult = new JSONObject(dmnResultJson);
            String decisionMessage = jsonResult.optString("decision", "Decision processed successfully").trim();
            String exceptionMessage = jsonResult.optString("exception", null);

            // Check if there is an exception from DMN
            if (exceptionMessage != null && !exceptionMessage.trim().isEmpty() && !"null".equals(exceptionMessage.trim())) {
                throw new CustomException(exceptionMessage);
            }

            // Log the DMN decision message (result)
            logger.info("DMN decision message: {}", decisionMessage);

            // Save the financial statement
            FinancialStatement entity = financialStatementMapper.toEntity(dto);
            entity.setCreatedAt(LocalDateTime.now());
            financialStatementRepository.save(entity);

            return "Financial statement evaluated and saved successfully. Result: " + decisionMessage;
        }
        catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }

    // Helper method to extract fields from 'actif' or 'passif'
    private void extractFields(Map<String, Object> rawData, String section, Map<String, Object> inputData) {
        Object sectionData = rawData.get(section);
        if (sectionData instanceof List) {
            List<?> groupList = (List<?>) sectionData;
            for (Object groupObj : groupList) {
                if (groupObj instanceof Map) {
                    Map<?, ?> group = (Map<?, ?>) groupObj;
                    if (group.containsKey("fields") && group.get("fields") instanceof List) {
                        List<?> fields = (List<?>) group.get("fields");
                        for (Object fieldObj : fields) {
                            if (fieldObj instanceof Map) {
                                Map<?, ?> field = (Map<?, ?>) fieldObj;
                                Object labelObj = field.get("label");
                                Object fieldValue = field.get("value");

                                // Process key to remove accents and spaces
                                if (labelObj != null) {
                                    String label = labelObj.toString();
                                    // Normalize and remove accents
                                    String key = Normalizer.normalize(label, Normalizer.Form.NFD)
                                            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                            .replaceAll("œ", "oe")
                                            .replaceAll("æ", "ae")
                                            .replaceAll(" +", "_"); // Replace spaces with underscores

                                    // Convert value to number if possible
                                    if (fieldValue instanceof String) {
                                        try {
                                            fieldValue = Double.parseDouble((String) fieldValue);
                                        } catch (NumberFormatException e) {
                                            // Keep as String if parsing fails
                                        }
                                    }
                                    inputData.put(key, fieldValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public List<FinancialStatementDTO> getAllFinancialStatements() {
        List<FinancialStatement> entities = financialStatementRepository.findAll();
        return entities.stream().map(financialStatementMapper::toDTO).toList();
    }

    public Optional<FinancialStatementDTO> getFinancialStatementById(Long id) {
        return financialStatementRepository.findById(id).map(financialStatementMapper::toDTO);
    }
}
