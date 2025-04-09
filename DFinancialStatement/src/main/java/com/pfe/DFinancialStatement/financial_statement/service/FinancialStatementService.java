package com.pfe.DFinancialStatement.financial_statement.service;

import com.pfe.DFinancialStatement.dmn_rule.service.DmnEvaluationService;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.mapper.FinancialStatementMapper;
import com.pfe.DFinancialStatement.financial_statement.repository.FinancialStatementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.report_generation.service.ReportGenerationService;
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

    @Autowired
    private ReportGenerationService reportGenerationService;


    public Map<String, Object> evaluateAndSaveStatement(FinancialStatementDTO dto, String ruleKey,String designName) {
        try {
            // Convert JSON string to Map
            Map<String, Object> rawData = objectMapper.readValue(dto.getFormData(), Map.class);
            Map<String, Object> inputData = new HashMap<>();

            extractFields(rawData, "actif", inputData);
            extractFields(rawData, "passif", inputData);

            // Evaluate DMN rule
            String dmnResultJson = dmnEvaluationService.evaluateDmn(ruleKey, inputData);

            // Parse decision result
            JSONObject jsonResult = new JSONObject(dmnResultJson);
            String decisionMessage = jsonResult.optString("decision", "Decision processed successfully").trim();
            String exceptionMessage = jsonResult.optString("exception", null);

            if (exceptionMessage != null && !exceptionMessage.trim().isEmpty() && !"null".equals(exceptionMessage.trim())) {
                throw new CustomException(exceptionMessage);
            }

            byte[] reportPdf = reportGenerationService.generateFinancialReport(rawData, "Company Name",designName);

            // Map DTO to entity and attach the report
            FinancialStatement entity = financialStatementMapper.toEntity(dto);
            entity.setReport(reportPdf);
            entity.setCreatedAt(LocalDateTime.now());

            // Call saveFinancialStatement method to save the financial statement
            saveFinancialStatement(dto.getFormData(), reportPdf); // Save the formData and report

            // Return result as JSON object
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Financial statement evaluated and saved successfully.");
            result.put("result", decisionMessage);

            return result;
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }

    public void saveFinancialStatement(String formData, byte[] reportBytes) {
        FinancialStatement statement = new FinancialStatement();
        statement.setFormData(formData);
        statement.setReport(reportBytes);
        financialStatementRepository.save(statement);  // This saves the statement in the DB
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
