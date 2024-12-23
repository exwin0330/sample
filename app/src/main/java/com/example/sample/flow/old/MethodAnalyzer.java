package com.example.sample.flow.old;

import com.example.sample.flow.BacktrackingResult;
import com.example.sample.flow.VariableClassification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class MethodAnalyzer {
    private final BacktrackingResult backtrackingResults;

    // 分類対象の変数を抽出
    private List<Value> classificationTargets = new ArrayList<>();

    private Map<String, Value> methodArgumentVariables = new HashMap<>();

    public MethodAnalyzer(BacktrackingResult backtrackingResults) {
        this.backtrackingResults = backtrackingResults;
    }

    public void analyzeMethod(SootMethod method, String targetMethodName) {
        System.out.println("Analyzing method: " + method.getSignature());
        if (!method.isConcrete()) {
            System.out.println("  Method is not concrete, skipping.");
            return;
        }

        Body body = method.retrieveActiveBody();
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

        for (Unit unit : body.getUnits()) {
            if (isTargetMethodUsage(unit, targetMethodName)) {
                Stmt stmt = (Stmt) unit;
                System.out.println("  Found usage in method: " + method.getSignature());
                System.out.println("  Usage: " + unit);

                // 解析: 引数や代入先変数の情報を取得
                analyzeStatementDetails(stmt, graph, true, method);

                // バックトラッキング処理など
                // backtrack(graph, unit, new HashSet<>(), 0, 10);
            } else {
                analyzeStatementDetails((Stmt) unit, graph, false, method);
            }
        }

        // 最後に drawableResId と theme の変数を出力
        System.out.println("\n--- Method Arguments (Jimple Variables) ---");
        if (methodArgumentVariables.containsKey("drawableResId")) {
            System.out.println("  drawableResId corresponds to: " + methodArgumentVariables.get("drawableResId"));
        } else {
            System.out.println("  drawableResId not found.");
        }

        if (methodArgumentVariables.containsKey("theme")) {
            System.out.println("  theme corresponds to: " + methodArgumentVariables.get("theme"));
        } else {
            System.out.println("  theme not found.");
        }
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

    private void analyzeStatementDetails(Stmt stmt, ExceptionalUnitGraph graph, boolean isTarget, SootMethod method) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            System.out.println("    InvokeExpr: " + invokeExpr);

            if (isTarget) {
                List<Value> args = invokeExpr.getArgs();
                for (Value arg : args) {
                    String sourceName = resolveSourceVariableName(graph, stmt, arg, method);

                    if (sourceName != null) {
                        System.out.println("      Source variable name: " + sourceName);
                    } else {
                        System.out.println("      Source variable name not found for: " + arg);
                    }
                }
            }
            /*
            // 引数の解析
            List<Value> args = invokeExpr.getArgs();
            for (Value arg : args) {
                System.out.println("arg: " + arg);
                if (arg instanceof Local || arg instanceof FieldRef || arg instanceof ParameterRef) {
                    classificationTargets.add(arg); // 引数として使用される変数を記録
                    System.out.println("      Argument: " + arg);

                    // ソースコードの変数名を取得するためのバックトラッキング
                    String sourceVariableName = resolveSourceVariableName(graph, stmt, arg);

                    if (sourceVariableName != null) {
                        System.out.println("      Source variable name: " + sourceVariableName);

                        // 特定した引数の変数名でチェック
                        if (sourceVariableName.equals("drawableResId")) {
                            methodArgumentVariables.put("drawableResId", arg);
                        } else if (sourceVariableName.equals("theme")) {
                            methodArgumentVariables.put("theme", arg);
                        }
                    }
                } else if (arg instanceof Constant) {
                    classificationTargets.add(arg); // 定数も追跡対象に追加
                    System.out.println("      Constant argument: " + arg);
                }
            }

            // 代入先変数の解析 (AssignStmtの場合)
            if (stmt instanceof AssignStmt) {
                Value leftOp = ((AssignStmt) stmt).getLeftOp();
                System.out.println("      Assigns to variable: " + leftOp);

                if (leftOp instanceof Local || leftOp instanceof FieldRef) {
                    classificationTargets.add(leftOp); // フィールド変数やローカル変数を記録
                }
            }

            // 分類処理
            for (Value targetVariable : classificationTargets) {
                backtrack(graph, stmt, new HashSet<>(), 0, 10, targetVariable);
            }
            */
        } else if (stmt instanceof AssignStmt) {
            // 非ターゲット文でのフィールド変数の解析
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            Value rightOp = ((AssignStmt) stmt).getRightOp();

            if (leftOp instanceof FieldRef || rightOp instanceof FieldRef) {
                System.out.println("    Field reference in statement: " + stmt);
                classificationTargets.add(leftOp);
                classificationTargets.add(rightOp);
            } else if (rightOp instanceof Constant) {
                // 定数代入を記録
                System.out.println("    Constant in statement: " + rightOp);
                classificationTargets.add(rightOp);
            }
        }
    }

    private String resolveSourceVariableName(ExceptionalUnitGraph graph, Stmt currentStmt, Value variable, SootMethod method) {
        // Jimple での変数定義をバックトラック
        for (Unit pred : graph.getPredsOf(currentStmt)) {
            if (pred instanceof DefinitionStmt) {
                DefinitionStmt defStmt = (DefinitionStmt) pred;
                Value leftOp = defStmt.getLeftOp();

                // 解析対象の変数と一致するか確認
                if (leftOp.equivTo(variable)) {
                    Value rightOp = defStmt.getRightOp();

                    // 引数の場合
                    if (rightOp instanceof ParameterRef) {
                        ParameterRef paramRef = (ParameterRef) rightOp;
                        int paramIndex = paramRef.getIndex();
                        return getSourceParameterName(paramIndex, method); // メソッド引数名を取得
                    }

                    // 定数の場合
                    if (rightOp instanceof Constant) {
                        return "Constant: " + rightOp.toString();
                    }

                    // さらにバックトラック
                    return resolveSourceVariableName(graph, (Stmt) pred, rightOp, method);
                }
            }
        }
        return null; // 変数名が特定できなかった場合
    }

    // ソースコードの引数名を取得するヘルパーメソッド
    private String getSourceParameterName(int index, SootMethod method) {
        if (!method.hasActiveBody()) {
            return "arg" + index + " (unknown)";
        }

        Body body = method.getActiveBody();
        List<Local> locals = new ArrayList<>();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) unit;
                Value leftOp = identityStmt.getLeftOp();
                Value rightOp = identityStmt.getRightOp();

                // 引数として識別されるものを収集
                if (rightOp instanceof ParameterRef) {
                    locals.add((Local) leftOp);
                }
            }
        }

        if (index >= 0 && index < locals.size()) {
            Local local = locals.get(index);
            return local.getName() + " (" + local.getType() + ")";
        }

        return "arg" + index + " (unknown)";
    }


    private void backtrack(ExceptionalUnitGraph graph, Unit currentUnit, Set<Unit> visited, int depth, int maxDepth, Value targetVariable) {
        if (depth > maxDepth || visited.contains(currentUnit)) return;
        visited.add(currentUnit);

        if (currentUnit instanceof DefinitionStmt) {
            DefinitionStmt defStmt = (DefinitionStmt) currentUnit;
            Value leftOp = defStmt.getLeftOp();
            Value rightOp = defStmt.getRightOp();

            String variableName = leftOp.toString();

            // フィールド変数を処理
            if (leftOp.equals(targetVariable) || rightOp.equals(targetVariable)) {
                VariableClassification.Type type;
                String initializationDetails = "";

                if (leftOp instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) leftOp;
                    System.out.println("        Field reference: " + fieldRef.getField().getName());
                    type = VariableClassification.Type.FIELD;
                    initializationDetails = "Field: " + fieldRef.getField().getName();
                } else if (rightOp instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) rightOp;
                    System.out.println("        Field reference: " + fieldRef.getField().getName());
                    type = VariableClassification.Type.FIELD;
                    initializationDetails = "Field: " + fieldRef.getField().getName();
                } else if (rightOp instanceof ParameterRef) {
                    type = VariableClassification.Type.ARGUMENT; // メソッド引数
                    initializationDetails = "Initialized from method argument";
                } else if (rightOp instanceof Constant) {
                    type = VariableClassification.Type.INITIALIZED; // 定数で初期化
                    initializationDetails = "Initialized with constant: " + rightOp;
                } else if (rightOp instanceof Local) {
                    type = VariableClassification.Type.INITIALIZED; // ローカル変数で初期化
                    initializationDetails = "Initialized from another variable: " + rightOp;
                } else {
                    type = VariableClassification.Type.OTHER; // その他
                    initializationDetails = "Initialization type unknown";
                }

                backtrackingResults.addResult(variableName, "Variable", currentUnit.toString());

                System.out.println("        Variable '" + leftOp + "' classified as: " + type);
                System.out.println("          Details: " + initializationDetails);
            }
        }

        // 次のユニットを再帰的に解析
        for (Unit pred : graph.getPredsOf(currentUnit)) {
            backtrack(graph, pred, visited, depth + 1, maxDepth, targetVariable);
        }
    }


}
