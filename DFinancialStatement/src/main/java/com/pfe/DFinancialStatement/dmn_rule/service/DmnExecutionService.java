package com.pfe.DFinancialStatement.dmn_rule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import com.pfe.DFinancialStatement.error_messages.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DmnExecutionService {

    private final DmnRuleRepository dmnRuleRepository;
    // API key et endpoint de l'API OpenRouter
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String OPENROUTER_API_KEY = "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";

    public DmnExecutionService(DmnRuleRepository dmnRuleRepository) {
        this.dmnRuleRepository = dmnRuleRepository;
    }

    public List<DmnRule> getAllDmnRules() {
        System.out.println("Fetching all DMN rules...");
        return dmnRuleRepository.findAll();
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

    /**
     * Normalise un nom de champ en enlevant les accents, en convertissant en minuscules
     * et en remplaçant les caractères non alphanumériques par des underscores.
     */
    private String normalizeFieldName(String rawName) {
        String withoutAccents = Normalizer.normalize(rawName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String normalizedName = withoutAccents.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");

        System.out.println("Normalized field name: " + rawName + " -> " + normalizedName);
        return normalizedName;
    }

    /**
     * Construit dynamiquement le prompt à envoyer à l'API AI en injectant les contenus JSON et XML.
     * Le prompt spécifie explicitement que la réponse doit être exactement un seul digit ("1" ou "0").
     */
    private String buildPrompt(String jsonContent, String xmlContent) {
        String promptTemplate = "A DMN and a JSON are considered compatible if and only if all input labels required by the DMN XML are present inside the JSON, considering normalization. Normalization rules include:\n\n" +
                " - Spaces (\" \") can be replaced by underscores (\"_\").\n" +
                " - Accented characters (e.g., \"é\") can be replaced by their non-accented equivalent (\"e\").\n" +
                " - Matching is case-insensitive (e.g., \"Logiciels\" = \"logiciels\").\n" +
                " - Extra fields in the JSON should be ignored.\n\n" +
                "Please determine whether the following DMN and JSON are compatible. Your response must be exactly one single digit: 1 if compatible, 0 if not.\n\n" +
                "This is the JSON: %s\n" +
                "This is the XML: %s";
        return String.format(promptTemplate, jsonContent, xmlContent);
    }

    /**
     * Appel à l'API OpenRouter pour vérifier la compatibilité.
     * Retourne true si compatible, false sinon.
     * La méthode réessaie l'appel si la réponse n'est pas exactement un seul digit ("1" ou "0").
     */
    private boolean isCompatibleWithAI(String jsonContent, String xmlContent) throws IOException, InterruptedException {
        String prompt = buildPrompt(jsonContent, xmlContent);

        // Construction de la charge utile (payload) en JSON
        String payload = String.format(
                "{\"model\": \"deepseek/deepseek-r1-zero:free\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
                prompt.replace("\"", "\\\"")
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + OPENROUTER_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Réponse de l'API AI: " + response.body());

        // Parse de la réponse avec Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            String answer = choices.get(0)
                    .path("message")
                    .path("content")
                    .asText()
                    .trim();
            System.out.println("Réponse extraite de l'AI : " + answer);

            // Si la réponse est exactement "1" ou "0", on la renvoie directement
            if ("1".equals(answer) || "0".equals(answer)) {
                return "1".equals(answer);
            }

            // Sinon, tenter d'extraire un digit isolé avec une expression régulière
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b([01])\\b");
            java.util.regex.Matcher matcher = pattern.matcher(answer);
            if (matcher.find()) {
                String extracted = matcher.group(1);
                System.out.println("Digit extrait après nettoyage : " + extracted);
                return "1".equals(extracted);
            } else {
                throw new IOException("La réponse de l'API AI ne contient pas de digit isolé valide.");
            }
        } else {
            throw new IOException("Aucun choix retourné par l'API AI.");
        }
    }



    /**
     * Vérifie la compatibilité de chaque DMN par rapport aux formFields (considérés comme JSON)
     * en utilisant l’API AI.
     */
    public List<DmnRule> findCompatibleDmnsWithAI(Set<String> formFields) {
        System.out.println("Finding compatible DMNs using AI...");
        List<DmnRule> compatible = new ArrayList<>();

        // Normalisation des formFields
        Set<String> normalizedFormFields = formFields.stream()
                .map(this::normalizeFieldName)
                .collect(Collectors.toSet());
        System.out.println("Normalized form fields: " + normalizedFormFields);

        // Génération du JSON à partir des formFields
        String jsonContent;
        try {
            jsonContent = new ObjectMapper().writeValueAsString(formFields);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la conversion des formFields en JSON", e);
        }

        // Pour chaque DMN, on envoie le JSON et le XML à l'API AI
        for (DmnRule rule : dmnRuleRepository.findAll()) {
            System.out.println("Processing DMN rule: " + rule.getRuleKey());
            String xmlContent = rule.getRuleContent();
            try {
                boolean isCompatible = isCompatibleWithAI(jsonContent, xmlContent);
                if (isCompatible) {
                    compatible.add(rule);
                    System.out.println("DMN rule " + rule.getRuleKey() + " is compatible according to AI.");
                } else {
                    System.out.println("DMN rule " + rule.getRuleKey() + " is not compatible according to AI.");
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Erreur lors de la vérification de la règle " + rule.getRuleKey() + ": " + e.getMessage());
            }
        }
        System.out.println("Found " + compatible.size() + " compatible DMNs.");
        return compatible;
    }
}
