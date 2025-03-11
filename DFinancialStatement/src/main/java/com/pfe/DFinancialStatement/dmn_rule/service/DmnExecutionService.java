package com.pfe.DFinancialStatement.dmn_rule.service;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DmnExecutionService {

    private final DmnRuleRepository dmnRuleRepository;
    private final DmnEngine dmnEngine;

    public DmnExecutionService(DmnRuleRepository dmnRuleRepository) {
        this.dmnRuleRepository = dmnRuleRepository;
        this.dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    }

    // Votre méthode existante d'évaluation de risque...
    public String evaluateRisk(String ruleKey, double ratioEndettement, double ratioLiquidite, double ratioSolvabilite) {
        DmnRule rule = dmnRuleRepository.findByRuleKey(ruleKey).orElse(null);
        if (rule == null) {
            return "Aucune règle trouvée pour la clé : " + ruleKey;
        }
        String dmnXml = rule.getRuleContent();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dmnXml.getBytes(StandardCharsets.UTF_8));
        // ... évaluation du DMN ...
        // (Code inchangé)
        return "Resultat"; // Exemple
    }

    // Méthode d'import existante...
    public DmnRule importDmn(String ruleKey, MultipartFile file) throws IOException {
        if (dmnRuleRepository.findByRuleKey(ruleKey).isPresent()) {
            throw new RuntimeException("Le rule_key existe déjà dans la base de données.");
        }
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        DmnRule dmnRule = new DmnRule();
        dmnRule.setRuleKey(ruleKey);
        dmnRule.setRuleContent(content);
        return dmnRuleRepository.save(dmnRule);
    }

    // Méthode pour extraire les inputs du DMN à l'aide d'un regex
    // Méthode pour extraire les inputs du DMN à l'aide d'un regex
    private Set<String> extractDmnInputs(String dmnContent) {
        Set<String> inputs = new HashSet<>();
        Pattern pattern = Pattern.compile("<inputExpression[^>]*>\\s*<text>(.*?)</text>\\s*</inputExpression>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(dmnContent);
        while (matcher.find()) {
            // Remplacer les espaces par des underscores pour uniformiser avec les formFields
            String input = matcher.group(1).trim().toLowerCase().replace(" ", "_");
            inputs.add(input);
            System.out.println("Extracted DMN input: " + input);
        }
        System.out.println("Total extracted DMN inputs: " + inputs);
        return inputs;
    }

    // Méthode pour trouver les DMN compatibles avec un ensemble de champs du template
    public List<DmnRule> findCompatibleDmns(Set<String> formFields) {
        // Normaliser les champs du template en minuscules
        Set<String> normalizedFields = formFields.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        System.out.println("Normalized form fields: " + normalizedFields);

        List<DmnRule> allDmns = dmnRuleRepository.findAll();
        List<DmnRule> compatible = new ArrayList<>();
        for (DmnRule rule : allDmns) {
            Set<String> dmnInputs = extractDmnInputs(rule.getRuleContent());
            System.out.println("Checking DMN Rule " + rule.getId() + " with inputs: " + dmnInputs);
            // Si tous les inputs du DMN sont contenus dans les champs du formulaire, le DMN est compatible
            if (normalizedFields.containsAll(dmnInputs)) {
                System.out.println("DMN Rule " + rule.getId() + " is compatible");
                compatible.add(rule);
            } else {
                // Log which inputs are missing
                Set<String> missingInputs = new HashSet<>(dmnInputs);
                missingInputs.removeAll(normalizedFields);
                System.out.println("DMN Rule " + rule.getId() + " is not compatible, missing inputs: " + missingInputs);
            }
        }
        System.out.println("Total compatible DMN rules found: " + compatible.size());
        return compatible;
    }

}
