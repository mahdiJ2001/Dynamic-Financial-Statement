package com.pfe.DFinancialStatement.dmn_rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.activity.enums.ActionType;
import com.pfe.DFinancialStatement.activity.service.ActivityLogService;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.AuthService;
import com.pfe.DFinancialStatement.dmn_rule.dto.RuleDto;
import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import com.pfe.DFinancialStatement.form_template.entity.FormTemplate;
import com.pfe.DFinancialStatement.form_template.repository.FormTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DmnRuleImportService {

    private final DmnRuleRepository dmnRuleRepository;

    @Autowired
    private FormTemplateRepository formTemplateRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private AuthService authService;

    public DmnRuleImportService(DmnRuleRepository dmnRuleRepository) {
        this.dmnRuleRepository = dmnRuleRepository;
    }

    public DmnRule importDmn(String ruleKey, MultipartFile file) throws IOException {
        System.out.println("Importing DMN rule with key: " + ruleKey);

        if (dmnRuleRepository.findByRuleKey(ruleKey).isPresent()) {
            throw new CustomException("RULE_KEY_EXISTS");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        DmnRule dmnRule = new DmnRule();
        dmnRule.setRuleKey(ruleKey);
        dmnRule.setRuleContent(content);

        System.out.println("DMN rule imported successfully: " + ruleKey);
        return dmnRuleRepository.save(dmnRule);
    }

    public List<DmnRule> getAllDmnRules() {
        return dmnRuleRepository.findAll();
    }

    public List<DmnRule> getDmnRulesByTemplateId(Long templateId) {
        return dmnRuleRepository.findByFormTemplateId(templateId);
    }


    public boolean existsByRuleKey(String ruleKey) {
        return dmnRuleRepository.findByRuleKey(ruleKey).isPresent();
    }

    public DmnRule saveNewDmnRule(Long templateId, String ruleKey, String ruleContent, List<RuleDto> ruleDtos) {
        if (dmnRuleRepository.findByRuleKey(ruleKey).isPresent()) {
            throw new CustomException("RULE_KEY_EXISTS");
        }

        FormTemplate formTemplate = formTemplateRepository.findById(templateId)
                .orElseThrow(() -> new CustomException("TEMPLATE_NOT_FOUND"));

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String ruleDtosJson = objectMapper.writeValueAsString(ruleDtos);

            DmnRule dmnRule = new DmnRule();
            dmnRule.setRuleKey(ruleKey);
            dmnRule.setRuleContent(ruleContent);
            dmnRule.setRuleDtosJson(ruleDtosJson);
            dmnRule.setFormTemplate(formTemplate);

            DmnRule savedRule = dmnRuleRepository.save(dmnRule);

            User currentUser = authService.getCurrentUser();
            activityLogService.log(
                    ActionType.CREATE_VALIDATION_MODEL,
                    "User " + currentUser.getUsername() + " created validation model with ruleKey: " + ruleKey
            );

            return savedRule;

        } catch (JsonProcessingException e) {
            throw new CustomException("FAILED_TO_SERIALIZE_RULE_DTO");
        }
    }



}
