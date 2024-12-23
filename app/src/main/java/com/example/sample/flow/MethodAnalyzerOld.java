package com.example.sample.flow;

import polyglot.ast.Assign;
import soot.*;
import soot.JastAddJ.AssignExpr;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.*;

public class MethodAnalyzerOld {
    //private final BacktrackingResult backtrackingResults;
    private final Map<Local, String> localVariableNames = new HashMap<>();

    private final Map<SootField, String> fieldVariableNames = new HashMap<>();

    private final Map<Value, SootField> localToFieldMapping = new HashMap<>();

    private final List<InitializationInfo> initializations = new ArrayList<>();

    public MethodAnalyzerOld(BacktrackingResult backtrackingResults) {
        // this.backtrackingResults = backtrackingResults;
    }

    public MethodAnalyzerOld() {}

    public void analyzeMethod(SootMethod method, String targetMethodName) {
        System.out.println("Analyzing method: " + method.getSignature());
        if (!method.isConcrete()) {
            System.out.println("  Method is not concrete, skipping.");
            return;
        }

        Body body = method.retrieveActiveBody();
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

        // ローカル変数名を収集
        collectLocalVariableNames(body);

        for (Unit unit : body.getUnits()) {
            if (isTargetMethodUsage(unit, targetMethodName)) {
                Stmt targetStmt = (Stmt) unit;
                System.out.println("  Found usage in method: " + method.getSignature());
                System.out.println("  Usage: " + unit);

                // バックトラッキングを開始
                Set<Unit> visited = new HashSet<>();
                backtrack(graph, targetStmt, visited, 0, 10, null, method);
            }

            if (unit instanceof DefinitionStmt) {
                DefinitionStmt defStmt = (DefinitionStmt) unit;
                if (defStmt.getLeftOp() instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) defStmt.getLeftOp();
                    SootField field = fieldRef.getField();
                    if (field.getName().equals("theme")) {
                        System.out.println("Field `theme` is initialized in method: " + method.getSignature());
                    }
                }
            }
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();

                // フィールドへの代入を検出
                if (leftOp instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) leftOp;
                    SootField field = fieldRef.getField();

                    // 初期化情報を保存
                    initializations.add(new InitializationInfo(field, unit, method));
                    System.out.println("Collected initialization: " + field.getName() +
                            " in method " + method.getSignature() +
                            " at statement " + unit);
                }
            }
        }
    }

    public void analyzeClass(SootClass sootClass, String targetMethodName) {
        System.out.println("Analyzing class: " + sootClass.getName());

        // フィールド変数の収集
        collectFieldVariables(sootClass);

        // メソッド解析
        for (SootMethod method : sootClass.getMethods()) {
            analyzeMethod(method, targetMethodName);
        }

        // 初期化情報の出力
        outputCollectedInitializations();
    }

    private boolean isTargetMethodUsage(Unit unit, String targetMethodName) {
        if (unit instanceof InvokeStmt) {
            InvokeStmt invokeStmt = (InvokeStmt) unit;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
            return invokeExpr.getMethod().getName().equals(targetMethodName);
        } else if (unit instanceof DefinitionStmt) {
            DefinitionStmt defStmt = (DefinitionStmt) unit;
            if (defStmt.getRightOp() instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) defStmt.getRightOp();
                return invokeExpr.getMethod().getName().equals(targetMethodName);
            }
        }
        return false;
    }

    // クラス内のフィールドを収集
    private void collectFieldVariables(SootClass sootClass) {
        for (SootField field : sootClass.getFields()) {
            fieldVariableNames.put(field, field.getName());
            System.out.println("Collected Field: " + field + " (name: " + field.getName() + ")");
        }
    }

    private void collectLocalVariableNames(Body body) {
        for (Local local : body.getLocals()) {
            String originalName = local.getName();

            // Jimple上で生成された一時変数を人間が理解できる形に変換
            if (originalName.startsWith("$")) {
                originalName = resolveOriginalVariableName(local, body);
            }

            localVariableNames.put(local, originalName);
            System.out.println("Collected Local: " + local + " (original name: " + originalName + ")");
        }
    }

    private String resolveOriginalVariableName(Local local, Body body) {
        // ユニットを走査し、ローカル変数に関する情報を解析
        for (Unit unit : body.getUnits()) {
            if (unit instanceof DefinitionStmt) {
                DefinitionStmt defStmt = (DefinitionStmt) unit;
                if (defStmt.getLeftOp().equals(local)) {
                    // 右辺に元の変数名がある場合に取得
                    Value rightOp = defStmt.getRightOp();
                    if (rightOp instanceof Local) {
                        return rightOp.toString();
                    }
                }
            }
        }
        return local.getName(); // 解決できなければ元の名前を返す
    }

    // バックトラッキングと解析を統合したメソッド
    private void backtrack(ExceptionalUnitGraph graph, Unit currentUnit, Set<Unit> visited,
                           int depth, int maxDepth, Value targetVariable, SootMethod method) {
        if (depth > maxDepth || visited.contains(currentUnit)) return;
        visited.add(currentUnit);

        if (currentUnit instanceof Stmt) {
            Stmt stmt = (Stmt) currentUnit;

            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                System.out.println("    InvokeExpr: " + invokeExpr);

                // 引数の解析
                for (Value arg : invokeExpr.getArgs()) {
                    if (targetVariable == null || targetVariable.equivTo(arg)) {
                        String formattedArg = formatVariable(arg);
                        System.out.println("      Backtracking argument: " + formattedArg);
                    }
                }
            } else if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();

                if (targetVariable == null || targetVariable.equivTo(leftOp)) {
                    System.out.println("    Analyzing assignment: " + assignStmt);

                    if (rightOp instanceof FieldRef) {
                        FieldRef fieldRef = (FieldRef) rightOp;
                        SootField field = fieldRef.getField();
                        String fieldName = fieldVariableNames.getOrDefault(field, field.getName());
                        System.out.println("      Field reference: " + fieldName);

                        // フィールド値のバックトラッキング（オプション）
                        backtrack(graph, currentUnit, visited, depth + 1, maxDepth, fieldRef, method);

                        if (leftOp instanceof Local) {
                            localToFieldMapping.put(leftOp, field);
                            System.out.println("      Mapping: Local variable " + leftOp + " originates from field " + field.getName());
                        }
                    } else if (leftOp instanceof FieldRef) {
                        FieldRef fieldRef = (FieldRef) leftOp;
                        SootField field = fieldRef.getField();
                        String fieldName = fieldVariableNames.getOrDefault(field, field.getName());
                        System.out.println("      Field assigned: " + fieldName);

                        // フィールドが更新されている場合の処理
                        backtrack(graph, currentUnit, visited, depth + 1, maxDepth, rightOp, method);

                        if (rightOp instanceof Local) {
                            localToFieldMapping.put(rightOp, field);
                            System.out.println("    Mapping: Field " + field.getName() + " updated from local variable " + rightOp);
                        }
                    } else if (rightOp instanceof Constant) {
                        System.out.println("      Detected constant initialization: " + rightOp);
                    } else {
                        // 右辺が他の変数の場合、再帰的に追跡
                        backtrack(graph, currentUnit, visited, depth + 1, maxDepth, rightOp, method);
                    }
                }
            }

            // メソッド呼び出しの検出
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                SootMethod invokedMethod = invokeExpr.getMethod();
                System.out.println("    Found method call: " + invokedMethod.getSignature());

                // 対象変数に関連する場合、メソッドを解析
                if (targetVariable == null || stmt.toString().contains(targetVariable.toString())) {
                    analyzeMethod(invokedMethod, null);
                }
            }
        }

        // 前のユニットを探索
        for (Unit pred : graph.getPredsOf(currentUnit)) {
            backtrack(graph, pred, visited, depth + 1, maxDepth, targetVariable, method);
        }
    }

    // スタック変数の解決
    private String formatVariable(Value variable) {
        if (variable instanceof Local) {
            Local local = (Local) variable;
            String originalName = localVariableNames.getOrDefault(local, local.getName());
            return local.getName() + " (original name: " + originalName + ")";
        }
        return variable.toString();
    }

    // 解析結果の出力
    public void printFieldLocalMappings() {
        for (Map.Entry<Value, SootField> entry : localToFieldMapping.entrySet()) {
            System.out.println("Local variable " + entry.getKey() + " is related to field " + entry.getValue().getName());
        }
    }

    // 最終出力形式に変換
    public void outputCollectedInitializations() {
        System.out.println("\n=== Formatted initializations ===\n");
        for (InitializationInfo info : initializations) {
            String fieldName = info.getField().getName();
            String fieldType = info.getField().getType().toString();
            String statement = info.getUnit().toString();
            String method = info.getMethod().getName();

            System.out.println(fieldType + " " + fieldName + ";");
            System.out.println("// From method: " + method);
            System.out.println(statement.replace("this.", ""));
        }
        System.out.println("\n======\n");
    }
}