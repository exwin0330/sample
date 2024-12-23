package com.example.sample.flow.map;

import soot.Unit;
import soot.Value;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SootAnalysis {
    // Sootで解析された変数と対応する情報を格納
    private final Map<String, String> sootVariableMap = new HashMap<>();

    public void analyzeMethod(ExceptionalUnitGraph graph) {
        for (Unit unit : graph) {
            // 特定のステートメント（例: virtualinvoke）を解析
            if (unit.toString().contains("virtualinvoke")) {
                // ステートメントの情報を抽出
                for (Value value : unit.getUseBoxes().stream().map(box -> box.getValue()).collect(Collectors.toList())) {
                    String sootVariable = value.toString();
                    // Sootで見つかった変数名をマッピング
                    sootVariableMap.put(sootVariable, "Unknown"); // ここで後にASTの変数名とマッピング
                }
            }
        }
    }

    public Map<String, String> getSootVariableMap() {
        return sootVariableMap;
    }
}
