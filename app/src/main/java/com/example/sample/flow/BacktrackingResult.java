package com.example.sample.flow;

import java.util.*;

public class BacktrackingResult {
    private final Map<String, Map<String, List<String>>> results = new HashMap<>();

    public void addResult(String key, String category, String statement) {
        results.computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(category, k -> new ArrayList<>())
                .add(statement);
    }

    public Map<String, Map<String, List<String>>> getResults() {
        return results;
    }

    public void printResults() {
        System.out.println("\nBacktracking Analysis Results:");
        System.out.println("====================================");

        for (Map.Entry<String, Map<String, List<String>>> entry : results.entrySet()) {
            String variableName = entry.getKey();
            Map<String, List<String>> categories = entry.getValue();

            System.out.println("- Variable: " + variableName);
            for (Map.Entry<String, List<String>> categoryEntry : categories.entrySet()) {
                String category = categoryEntry.getKey();
                List<String> usages = categoryEntry.getValue();

                System.out.println("    Category: " + category);
                usages.stream()
                        .distinct()
                        .forEach(usage -> System.out.println("        - " + usage));
            }
        }
    }
}
