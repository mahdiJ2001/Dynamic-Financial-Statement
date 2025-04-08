package com.pfe.DFinancialStatement.dmn_rule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class DmnRuleAICompatibilityService {

    private final DmnRuleRepository dmnRuleRepository;
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String OPENROUTER_API_KEY = "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";  // Utilise ta clé API

    public DmnRuleAICompatibilityService(DmnRuleRepository dmnRuleRepository) {
        this.dmnRuleRepository = dmnRuleRepository;
    }

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

    private String buildPrompt(String jsonContent, String xmlContent) {
        String promptTemplate = "A DMN and a JSON are considered compatible if and only if all input labels required by the DMN XML are present inside the JSON, considering normalization. Normalization rules include:\n\n" +
                " - Spaces (\" \") can be replaced by underscores (\"_\").\n" +
                " - Accented characters (e.g., \"é\") can be replaced by their non-accented equivalent (\"e\").\n" +
                " - Matching is case-insensitive (e.g., \"Logiciels\" = \"logiciels\").\n" +
                " - Extra fields in the JSON should be ignored.\n\n" +
                "Please determine whether the following DMN and JSON are compatible. Your response must be exactly one single digit: 1 if compatible, 0 if not. Answer very briefly (only a digit, no explanation).\n\n" +
                "This is the JSON: %s\n" +
                "This is the XML: %s";
        return String.format(promptTemplate, jsonContent, xmlContent);
    }

    private boolean isCompatibleWithAI(String jsonContent, String xmlContent) throws IOException, InterruptedException {
        String prompt = buildPrompt(jsonContent, xmlContent);

        // Construction de la charge utile (payload) en JSON
        String payload = String.format(
                "{\"model\": \"deepseek/deepseek-r1-distill-llama-70b:free\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
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

            if ("1".equals(answer) || "0".equals(answer)) {
                return "1".equals(answer);
            }

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

        // ExecutorService pour l'exécution parallèle des requêtes API
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        // Pour chaque DMN, on crée une tâche qui enverra le JSON et le XML à l'API AI
        for (DmnRule rule : dmnRuleRepository.findAll()) {
            tasks.add(() -> {
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
                return true;
            });
        }

        try {
            // Exécution parallèle des tâches
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Execution interrupted: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }

        System.out.println("Found " + compatible.size() + " compatible DMNs.");
        return compatible;
    }
}
