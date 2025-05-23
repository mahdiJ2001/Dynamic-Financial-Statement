package com.pfe.DFinancialStatement.utils;

public class ExpressionUtils {

    public static String normalizeExpression(String input) {
        if (input == null) return "";
        String[] tokens = input.split("(?=[-+*/<>=()])|(?<=[-+*/<>=()])");

        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.matches("[\\p{L}0-9_]+(\\s+[\\p{L}0-9_]+)+")) {
                result.append(trimmed.replaceAll("\\s+", "_"));
            } else {
                result.append(trimmed);
            }
        }

        return result.toString();
    }
}
