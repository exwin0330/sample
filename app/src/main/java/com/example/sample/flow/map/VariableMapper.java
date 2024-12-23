package com.example.sample.flow.map;

import java.util.HashMap;
import java.util.Map;

public class VariableMapper {
    public static Map<String, String> mapVariables(
            Map<String, String> sootVariables,
            Map<String, String> astVariables) {

        Map<String, String> variableMapping = new HashMap<>();

        // Soot変数とAST変数をマッピング
        for (Map.Entry<String, String> sootEntry : sootVariables.entrySet()) {
            String sootVar = sootEntry.getKey();

            // 簡単な一致アルゴリズム（必要に応じて改良可能）
            for (String astVar : astVariables.keySet()) {
                if (sootVar.contains(astVar)) { // 部分一致を試みる
                    variableMapping.put(sootVar, astVar);
                    break;
                }
            }
        }
        return variableMapping;
    }
}

