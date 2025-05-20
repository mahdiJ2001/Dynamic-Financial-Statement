package com.pfe.DFinancialStatement.financial_statement.service;

import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.AuthService;
import com.pfe.DFinancialStatement.dmn_rule.dto.ExpressionEvaluationResult;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnEvaluationService;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.entity.StatementStatus;
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
import java.util.*;

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

    @Autowired
    private AuthService authService;


    public List<ExpressionEvaluationResult> evaluateAndSaveStatement(FinancialStatementDTO dto, String ruleKey, String designName) {
        List<ExpressionEvaluationResult> evaluationResults = new ArrayList<>();

        try {
            Map<String, Object> rawData = objectMapper.readValue(dto.getFormData(), Map.class);
            Map<String, Object> inputData = new HashMap<>();

            extractFields(rawData, "actif", inputData);
            extractFields(rawData, "passif", inputData);

            String companyName = (String) rawData.get("companyName");

            evaluationResults = dmnEvaluationService.evaluateDmn(ruleKey, inputData);

            String evaluationJson = objectMapper.writeValueAsString(evaluationResults);

            boolean hasBlockingError = evaluationResults.stream()
                    .anyMatch(result -> "bloquant".equalsIgnoreCase(result.getSeverite()));

            if (hasBlockingError) {
                return evaluationResults;
            }

            byte[] reportPdf = reportGenerationService.generateFinancialReport(rawData, companyName, designName);

            FinancialStatement entity = financialStatementMapper.toEntity(dto);
            entity.setReport(reportPdf);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setCompanyName(companyName);
            entity.setEvaluationResult(evaluationJson);

            User currentUser = authService.getCurrentUser();
            entity.setCreatedBy(currentUser);

            financialStatementRepository.save(entity);

        } catch (Exception e) {
            logger.error("Error during financial statement evaluation and saving", e);

        }

        return evaluationResults;
    }


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
                                            .replaceAll(" +", "_");

                                    // Convert value to number if possible
                                    if (fieldValue instanceof String) {
                                        try {
                                            fieldValue = Double.parseDouble((String) fieldValue);
                                        } catch (NumberFormatException e) {
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

    public Map<String, Object> updateStatus(Long id, String status, String rejectionCause) {
        try {
            // Convert the status string to a StatementStatus enum
            StatementStatus statementStatus = StatementStatus.valueOf(status.toUpperCase());

            // Fetch the financial statement by ID
            Optional<FinancialStatement> financialStatementOpt = financialStatementRepository.findById(id);

            if (financialStatementOpt.isPresent()) {
                FinancialStatement financialStatement = financialStatementOpt.get();
                financialStatement.setStatus(statementStatus);

                // If the status is REJECTED, set the rejection cause
                if (statementStatus == StatementStatus.REJECTED && rejectionCause != null) {
                    financialStatement.setRejectionCause(rejectionCause);
                }

                // Save the updated financial statement
                financialStatementRepository.save(financialStatement);

                // Prepare and return the success response
                Map<String, Object> result = new HashMap<>();
                result.put("status", "success");
                result.put("message", "Financial statement status updated successfully.");
                return result;
            } else {
                throw new CustomException("Financial statement not found.");
            }
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid status value provided.");
        } catch (Exception e) {
            logger.error("Error updating financial statement status", e);
            throw new CustomException("Error updating status: " + e.getMessage());
        }
    }


}
