package com.pfe.DFinancialStatement.financial_statement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.pfe.DFinancialStatement.activity.enums.ActionType;
import com.pfe.DFinancialStatement.activity.service.ActivityLogService;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.AuthService;
import com.pfe.DFinancialStatement.dmn_rule.dto.ExpressionEvaluationResult;
import com.pfe.DFinancialStatement.dmn_rule.dto.RuleDto;
import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.service.DmnEvaluationService;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import com.pfe.DFinancialStatement.financial_statement.dto.FinancialStatementDTO;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatement;
import com.pfe.DFinancialStatement.financial_statement.entity.FinancialStatementMessage;
import com.pfe.DFinancialStatement.financial_statement.entity.StatementStatus;
import com.pfe.DFinancialStatement.financial_statement.mapper.FinancialStatementMapper;
import com.pfe.DFinancialStatement.financial_statement.repository.FinancialStatementRepository;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.notification.service.NotificationService;
import com.pfe.DFinancialStatement.outbox.service.OutboxEventService;
import com.pfe.DFinancialStatement.report_generation.service.ReportGenerationService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private ObjectMapper objectMapper; 

    @Autowired
    private ReportGenerationService reportGenerationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private DmnRuleRepository dmnRuleRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private OutboxEventService outboxEventService;



    public List<ExpressionEvaluationResult> evaluateAndSaveStatement(FinancialStatementDTO dto, String ruleKey, String designName) {
        List<ExpressionEvaluationResult> evaluationResults = new ArrayList<>();

        try {
            logger.info("Début de l'évaluation et sauvegarde du bilan financier.");

            // 1. Lecture des données du formulaire
            logger.info("Lecture des données du formulaire...");
            Map<String, Object> rawData = objectMapper.readValue(dto.getFormData(), Map.class);
            Map<String, Object> inputData = new HashMap<>();

            extractFields(rawData, "actif", inputData);
            extractFields(rawData, "passif", inputData);

            String companyName = (String) rawData.get("companyName");
            logger.info("Nom de la société extrait: {}", companyName);

            // 2. Récupérer la règle DMN et la liste des RuleDto depuis la base
            logger.info("Recherche de la règle DMN avec la clé: {}", ruleKey);
            DmnRule dmnRule = dmnRuleRepository.findByRuleKey(ruleKey)
                    .orElseThrow(() -> new RuntimeException("Règle DMN introuvable pour la clé: " + ruleKey));

            List<RuleDto> allRules = objectMapper.readValue(
                    dmnRule.getRuleDtosJson(),
                    new TypeReference<List<RuleDto>>() {}
            );
            logger.info("Nombre de règles DMN chargées: {}", allRules.size());

            // 3. Évaluation des règles
            logger.info("Évaluation des règles DMN...");
            evaluationResults = dmnEvaluationService.evaluateDmn(ruleKey, allRules, inputData);
            logger.info("Évaluation terminée, {} résultats obtenus.", evaluationResults.size());

            String evaluationJson = objectMapper.writeValueAsString(evaluationResults);

            boolean hasBlockingError = evaluationResults.stream()
                    .filter(result -> {
                        String sev = result.getSeverite().toLowerCase();
                        return sev.equals("bloquant") || sev.equals("blocking");
                    })
                    .anyMatch(result -> Boolean.TRUE.equals(result.isResult()));

            logger.info("Présence d'erreur bloquante ? {}", hasBlockingError);


            // 4. Si erreur bloquante, retourner directement les résultats sans générer de rapport
            if (hasBlockingError) {
                logger.warn("Erreurs bloquantes détectées, arrêt de la sauvegarde.");
                return evaluationResults;
            }

            // 5. Génération du rapport
            logger.info("Génération du rapport financier en PDF...");
            byte[] reportPdf = reportGenerationService.generateFinancialReport(rawData, companyName, designName);
            logger.info("Rapport généré, taille (bytes): {}", reportPdf.length);

            // 6. Enregistrement du bilan
            logger.info("Mapping du DTO vers l'entité FinancialStatement...");
            FinancialStatement entity = financialStatementMapper.toEntity(dto);

            entity.setReport(reportPdf);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setCompanyName(companyName);
            entity.setEvaluationResult(evaluationJson);

            User currentUser = authService.getCurrentUser();
            logger.info("Utilisateur courant récupéré: {}", currentUser.getUsername());
            entity.setCreatedBy(currentUser);

            logger.info("Sauvegarde de l'entité FinancialStatement en base...");
            financialStatementRepository.save(entity);
            logger.info("Sauvegarde réussie.");


            activityLogService.log(
                    ActionType.SUBMIT_REPORT,
                    "REPORT_SUBMITTED",
                    Map.of("username", currentUser.getUsername(), "companyName", companyName)
            );

        } catch (Exception e) {
            logger.error("Erreur lors de l'évaluation et sauvegarde du bilan financier", e);
            throw new RuntimeException("Erreur lors de l'évaluation et sauvegarde du bilan financier", e);
        }

        return evaluationResults;
    }

    public List<ExpressionEvaluationResult> evaluateWithoutSaving(FinancialStatementDTO dto, String ruleKey, String designName) throws JsonProcessingException {
        logger.info("Début de la prévisualisation du bilan financier sans sauvegarde.");

        Map<String, Object> rawData;
        try {
            rawData = objectMapper.readValue(dto.getFormData(), Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture des données du formulaire", e);
        }

        Map<String, Object> inputData = new HashMap<>();
        extractFields(rawData, "actif", inputData);
        extractFields(rawData, "passif", inputData);

        DmnRule dmnRule = dmnRuleRepository.findByRuleKey(ruleKey)
                .orElseThrow(() -> new RuntimeException("Règle DMN introuvable pour la clé: " + ruleKey));

        List<RuleDto> allRules;
        try {
            allRules = objectMapper.readValue(dmnRule.getRuleDtosJson(), new TypeReference<List<RuleDto>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture des règles DMN", e);
        }

        return dmnEvaluationService.evaluateDmn(ruleKey, allRules, inputData);

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
            StatementStatus statementStatus = StatementStatus.valueOf(status.toUpperCase());
            Optional<FinancialStatement> financialStatementOpt = financialStatementRepository.findById(id);

            if (financialStatementOpt.isPresent()) {
                FinancialStatement financialStatement = financialStatementOpt.get();
                financialStatement.setStatus(statementStatus);

                User user = financialStatement.getCreatedBy();
                User currentUser = authService.getCurrentUser();

                // Enregistre d'abord le changement d'état
                financialStatementRepository.save(financialStatement);

                if (statementStatus == StatementStatus.REJECTED) {
                    // Replace notificationService and activityLogService with Outbox
                    outboxEventService.saveEvent(
                            "Notification",
                            "REPORT_REJECTED",
                            Map.of("userId", user.getId().toString(), "companyName", financialStatement.getCompanyName())
                    );

                    outboxEventService.saveEvent(
                            "ActivityLog",
                            "REPORT_REJECTED_LOG",   // fixed from REJECT_REPORT_LOG
                            Map.of(
                                    "username", currentUser.getUsername(),
                                    "companyName", financialStatement.getCompanyName()
                            )
                    );

                } else if (statementStatus == StatementStatus.VALIDATED) {
                    outboxEventService.saveEvent(
                            "Notification",
                            "REPORT_VALIDATED",
                            Map.of("userId", user.getId().toString(), "companyName", financialStatement.getCompanyName())
                    );

                    outboxEventService.saveEvent(
                            "ActivityLog",
                            "REPORT_VALIDATED_LOG",  // fixed from VALIDATE_REPORT_LOG
                            Map.of(
                                    "username", currentUser.getUsername(),
                                    "companyName", financialStatement.getCompanyName()
                            )
                    );
                }

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



}
