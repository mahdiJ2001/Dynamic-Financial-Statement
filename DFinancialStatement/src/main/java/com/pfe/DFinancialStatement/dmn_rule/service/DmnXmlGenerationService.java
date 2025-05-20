package com.pfe.DFinancialStatement.dmn_rule.service;

import com.pfe.DFinancialStatement.dmn_rule.dto.RuleDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DmnXmlGenerationService {

    public String generateDmnXmlFromRuleDtoList(List<RuleDto> ruleDtos) {
        // Récupérer la liste unique des expressions dans l’ordre
        List<String> expressions = ruleDtos.stream()
                .map(RuleDto::getExpression)
                .distinct()
                .toList();

        // Grouper les règles par messageErreur + severite (car ce sont les identifiants des règles)
        Map<String, Map<String, String>> groupedConditions = new LinkedHashMap<>();
        Map<String, String> severities = new HashMap<>();

        for (RuleDto dto : ruleDtos) {
            String key = dto.getMessageErreur();
            groupedConditions.putIfAbsent(key, new LinkedHashMap<>());
            String fullCondition = dto.getCondition() + dto.getValue();
            groupedConditions.get(key).put(dto.getExpression(), fullCondition);
            severities.put(key, dto.getSeverite());
        }

        // Génération XML
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\"\n")
                .append("             xmlns:feel=\"https://www.omg.org/spec/DMN/20191111/FEEL/\"\n")
                .append("             xmlns:camunda=\"http://camunda.org/schema/1.0/dmn\"\n")
                .append("             id=\"definitions\"\n")
                .append("             name=\"definitions\"\n")
                .append("             namespace=\"http://camunda.org/schema/1.0/dmn\">\n\n");

        xml.append("  <decision id=\"decision\" name=\"Validation_Bilan\">\n");
        xml.append("    <decisionTable id=\"decisionTable\">\n\n");

        // Inputs
        for (int i = 0; i < expressions.size(); i++) {
            String expr = expressions.get(i);
            String normalized = normalizeExpression(expr);
            xml.append("      <input id=\"input_").append(i + 1).append("\" label=\"Expression ").append(i + 1).append("\">\n")
                    .append("        <inputExpression id=\"inputExpression_").append(i + 1).append("\" typeRef=\"double\">\n")
                    .append("          <text>").append(escapeXml(normalized)).append("</text>\n")
                    .append("        </inputExpression>\n")
                    .append("      </input>\n\n");
        }

        // Outputs
        xml.append("      <output id=\"output1\" label=\"Message d'erreur\" name=\"messageErreur\" typeRef=\"string\" />\n");
        xml.append("      <output id=\"output2\" label=\"Sévérité\" name=\"severite\" typeRef=\"string\" />\n\n");

        // Rules
        int ruleIndex = 1;
        for (Map.Entry<String, Map<String, String>> entry : groupedConditions.entrySet()) {
            String messageErreur = entry.getKey();
            String severite = severities.get(messageErreur);
            Map<String, String> conditionMap = entry.getValue();

            xml.append("      <rule id=\"rule_").append(ruleIndex).append("\">\n");

            for (String expr : expressions) {
                String cond = conditionMap.getOrDefault(expr, "-");
                xml.append("        <inputEntry id=\"inputEntry_")
                        .append(expressions.indexOf(expr) + 1).append("_").append(ruleIndex).append("\">\n")
                        .append("          <text>").append(escapeXml(cond)).append("</text>\n")
                        .append("        </inputEntry>\n");
            }

            xml.append("        <outputEntry id=\"outputEntry1_").append(ruleIndex).append("\">\n")
                    .append("          <text>\"").append(escapeXml(messageErreur)).append("\"</text>\n")
                    .append("        </outputEntry>\n")
                    .append("        <outputEntry id=\"outputEntry2_").append(ruleIndex).append("\">\n")
                    .append("          <text>\"").append(escapeXml(severite)).append("\"</text>\n")
                    .append("        </outputEntry>\n");

            xml.append("      </rule>\n\n");
            ruleIndex++;
        }

        xml.append("    </decisionTable>\n");
        xml.append("  </decision>\n");
        xml.append("</definitions>\n");

        return xml.toString();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Remplace les espaces uniquement dans les noms de variables (pas autour des opérateurs).
     */
    private String normalizeExpression(String input) {
        if (input == null) return "";

        // Séparer les opérateurs et parenthèses pour les identifier individuellement
        String[] tokens = input.split("(?=[-+*/<>=()])|(?<=[-+*/<>=()])");

        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            String trimmed = token.trim();

            // Si le token est un mot ou une suite de mots (e.g. "Dettes fournisseurs")
            if (trimmed.matches("[\\p{L}0-9_]+(\\s+[\\p{L}0-9_]+)+")) {
                result.append(trimmed.replaceAll("\\s+", "_"));
            } else {
                result.append(trimmed);
            }
        }

        return result.toString();
    }
}
