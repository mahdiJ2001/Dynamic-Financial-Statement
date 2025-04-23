package com.pfe.DFinancialStatement.dmn_rule.service;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DmnRuleImportService {

    private final DmnRuleRepository dmnRuleRepository;

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
}
