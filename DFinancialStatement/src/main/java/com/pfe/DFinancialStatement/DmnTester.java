package com.pfe.DFinancialStatement;

import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;

import java.io.InputStream;

public class DmnTester {
    public static void main(String[] args) {
        // Créer le moteur DMN
        DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

        // Charger le fichier DMN
        InputStream inputStream = DmnTester.class.getResourceAsStream("/RiskAssessment.dmn");
        if (inputStream == null) {
            System.err.println("Fichier DMN non trouvé ! Assurez-vous qu'il est dans le dossier resources.");
            return;
        }

        // Parser la décision DMN
        DmnDecision decision = dmnEngine.parseDecision("RiskAssessmentDecision", inputStream);

        // Tester plusieurs scénarios
        testScenario(dmnEngine, decision, 2.5, 0.8, 0.9); // Risque Élevé
        testScenario(dmnEngine, decision, 1.8, 0.9, 1.0); // Risque Modéré
        testScenario(dmnEngine, decision, 0.8, 1.5, 2.0); // Risque Faible
        testScenario(dmnEngine, decision, 3.0, 1.1, 1.2); // Risque Élevé (Endettement seul)
        testScenario(dmnEngine, decision, 1.0, 1.0, 1.0); // Non classé
    }

    private static void testScenario(DmnEngine dmnEngine, DmnDecision decision,
                                     double ratioEndettement, double ratioLiquidite, double ratioSolvabilite) {
        // Définir les variables d'entrée
        VariableMap variables = Variables.createVariables()
                .putValue("ratioEndettement", ratioEndettement)
                .putValue("ratioLiquiditeGenerale", ratioLiquidite)
                .putValue("ratioSolvabilite", ratioSolvabilite);

        // Afficher les variables passées au DMN pour débogage
        System.out.println("Variables passées au DMN :");
        System.out.println("  - ratioEndettement : " + variables.get("ratioEndettement"));
        System.out.println("  - ratioLiquiditeGenerale : " + variables.get("ratioLiquiditeGenerale"));
        System.out.println("  - ratioSolvabilite : " + variables.get("ratioSolvabilite"));

        // Évaluer la table DMN
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);

        // Afficher le résultat
        System.out.println("Scénario :");
        System.out.println("  - Ratio Endettement : " + ratioEndettement);
        System.out.println("  - Ratio Liquidité Générale : " + ratioLiquidite);
        System.out.println("  - Ratio Solvabilité : " + ratioSolvabilite);
        if (result.isEmpty()) {
            System.out.println("  - Risque : Aucune règle correspondante trouvée !");
        } else {
            System.out.println("  - Risque : " + result.getSingleResult().get("Risk"));
            // Utilisez "Risk" ici
        }
        System.out.println();
    }
}