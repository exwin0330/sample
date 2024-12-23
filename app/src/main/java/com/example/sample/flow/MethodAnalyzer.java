package com.example.sample.flow;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.*;

public class MethodAnalyzer {
    private final String argumentName;
    private final Map<Local, String> localVariableNames = new HashMap<>();
    private final Map<SootField, String> fieldVariableNames = new HashMap<>();
    private final Map<Value, SootField> localToFieldMapping = new HashMap<>();
    private final Set<InitializationInfo> initializations = new HashSet<>();
    private final Map<String, Object> returnValueMap = new HashMap<>();
    private final int maxDebugLevel = 1;


    public MethodAnalyzer(String variableName) {
        this.argumentName = variableName;
    }

    public void analyzeMethod(SootMethod method, String targetMethodName) {
        // 外部クラスをスキップする条件
        String className = method.getDeclaringClass().getName();
        if (isExternalClass(className)) {
            return;
        }

        debugPrint("Analyzing method: " + method.getSignature(), 1);

        if (!method.isConcrete()) {
            debugPrint("  Method is not concrete, skipping.", 1);
            return;
        }

        Body body = method.retrieveActiveBody();
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

        if (method.getName().equals(argumentName)) {
            debugPrint("Analyzing return value of method: " + argumentName, 1);

            // `ReturnValueAnalyzer`を利用して戻り値を解析
            Object rv = ReturnValueAnalyzer.analyzeReturnValue(method);

            if (rv != null) {
                debugPrint("Return value found: " + rv, 1);
                returnValueMap.put(method.getName(), rv);
            } else {
                debugPrint("No return value found for method: " + targetMethodName, 1);
            }

            for (Unit unit : body.getUnits()) {
                if (unit instanceof ReturnStmt) {
                    ReturnStmt returnStmt = (ReturnStmt) unit;
                    Value returnValue = returnStmt.getOp();

                    if (returnValue instanceof Constant) {
                        debugPrint("Return value is constant: " + returnValue, 1);
                    } else {
                        // `backtrack`を呼び出して戻り値の由来を追跡
                        Set<Unit> visited = new HashSet<>();
                        backtrack(graph, unit, visited, 0, 10, returnValue, method);
                    }
                }
            }
        }

        System.out.println("****** units ******");
        for (Unit unit : body.getUnits()) {
            System.out.println("Unit: " + unit);
        }
        System.out.println("******************");

            for (Unit unit : body.getUnits()) {
            boolean isMethodMatch = false;
            boolean isVariableMatch = false;

            if (isTargetMethodUsage(unit, targetMethodName)) {
                isMethodMatch = true;
                Stmt targetStmt = (Stmt) unit;
                debugPrint("  Found usage in method: " + method.getSignature(), 1);
                debugPrint("  Usage: " + unit, 1);

                Set<Unit> visited = new HashSet<>();
                backtrack(graph, (Stmt) unit, visited, 0, 10, null, method);
            }

            // メソッド名としての一致判定
            if (isTargetMethodUsage(unit, argumentName)) {
                isMethodMatch = true;
                debugPrint("  Detected `variableName` as a method name: " + argumentName, 1);
                debugPrint("  Usage: " + unit, 1);
            }

            if (unit instanceof DefinitionStmt) {
                DefinitionStmt defStmt = (DefinitionStmt) unit;
                if (defStmt.getLeftOp() instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) defStmt.getLeftOp();
                    SootField field = fieldRef.getField();
                    if (field.getName().equals(argumentName)) {
                        isVariableMatch = true;
                        debugPrint("Field `" + argumentName + "` is initialized in method: " + method.getSignature(), 1);
                    }
                } else if (defStmt.getLeftOp().toString().equals(argumentName)) {
                    isVariableMatch = true;
                    debugPrint("  Detected `variableName` as a variable name: " + argumentName, 1);
                }
            }

            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();

                // フィールドへの代入を検出
                if (leftOp instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) leftOp;
                    SootField field = fieldRef.getField();

                    // 重複しないように初期化情報を保存
                    InitializationInfo info = new InitializationInfo(field, unit, method);
                    if (!initializations.contains(info)) {
                        initializations.add(info);
                        debugPrint("Collected initialization: " + field.getName() +
                                           " in method " + method.getSignature() +
                                           " at statement " + unit, 1);
                    }
                }
            }

            // 両方のケースで一致した場合の特別な出力
            if (isMethodMatch && isVariableMatch) {
                debugPrint("  Warning: `variableName` matches both a method name and a variable/field name!", 0);
            }

            // バックトラッキング処理（メソッド名に一致した場合のみ）
            if (isMethodMatch) {
                Set<Unit> visited = new HashSet<>();
                backtrack(graph, (Stmt) unit, visited, 0, 10, null, method);
            }
        }
    }

    public void analyzeClassWithMethodName(SootClass sootClass, String targetMethodName, String methodName) {
        debugPrint("Analyzing class: " + sootClass.getName(), 1);

        // フィールド変数の収集
        collectFieldVariables(sootClass);

        // メソッド解析
        for (SootMethod method : sootClass.getMethods()) {
            if (method.getName().equals(methodName) || method.getName().equals("")) {
                analyzeMethod(method, targetMethodName);
            }
        }

        // 初期化情報の出力
        outputAnalysisLog();
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

    // 外部クラスを判定するヘルパーメソッド
    private boolean isExternalClass(String className) {
        // 例えば以下のような条件で外部クラスをスキップ
        return className.startsWith("android.") ||
                className.startsWith("java.") ||
                className.startsWith("kotlin.") ||
                className.startsWith("javax.") ||
                className.startsWith("sun.");
    }

    // クラス内のフィールドを収集
    private void collectFieldVariables(SootClass sootClass) {
        for (SootField field : sootClass.getFields()) {
            fieldVariableNames.put(field, field.getName());
            debugPrint("Collected Field: " + field + " (name: " + field.getName() + ")", 0);

            // 宣言時の初期化を解析
            if (field.getName().equals(argumentName)) {
                InitializationInfo info = new InitializationInfo(field, null, null);
                if (FieldInitializationChecker.checkFieldInitialization(field, sootClass, info)) {
                    // 初期化情報を取得
                    initializations.add(info);
                    debugPrint("Field " + field.getName() + " initialized at declaration with value: " + info.getInitializationValue(), 1);
                }
            }
        }
    }

    // バックトラッキングと解析を統合したメソッド
    private void backtrack(ExceptionalUnitGraph graph, Unit currentUnit, Set<Unit> visited,
                           int depth, int maxDepth, Value targetVariable, SootMethod method) {
        if (depth > maxDepth || visited.contains(currentUnit)) return;
        visited.add(currentUnit);

        if (!(currentUnit instanceof Stmt)) return;

        Stmt stmt = (Stmt) currentUnit;

        if (targetVariable != null && !stmt.toString().contains(targetVariable.toString())) return;

        if (targetVariable instanceof FieldRef) {
            FieldRef fieldRef = (FieldRef) targetVariable;
            SootField field = fieldRef.getField();
            debugPrint("Field variable detected: " + field.getName(), 1);

            // メソッド内のすべての呼び出しメソッドを探索
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                SootMethod invokedMethod = invokeExpr.getMethod();

                debugPrint("Analyzing invoked method for field initialization: " + invokedMethod.getSignature(), 1);
                if (invokedMethod.isConcrete()) {
                    analyzeMethodForFieldInitialization(invokedMethod, field);
                }
            }
            return;
        }

        debugPrint("backtrack: " + currentUnit.toString(), 1);

        if (stmt instanceof AssignStmt) {
            debugPrint("  ::assign stmt", 1);
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();

            // 対象変数の代入文か確認
            if (targetVariable == null || targetVariable.equivTo(leftOp)) {
                debugPrint("    Detected assignment to target variable: " + leftOp, 0);
                debugPrint("    RightOp class: " + rightOp.getClass().getName(), 0);
                debugPrint("    RightOp value: " + rightOp, 0);

                // 右辺がリテラルか、メソッド呼び出しによる初期化かを確認
                if (targetVariable != null && targetVariable.equivTo(leftOp)) {
                    debugPrint("Initialization detected: " + stmt, 1);
                }

                // 左辺がローカル変数の場合
                if (leftOp instanceof Local) {
                    Local localVar = (Local) leftOp;
                    debugPrint("Local variable detected on left-hand side: " + localVar.getName(), 1);

                    // ローカル変数の初期化情報を収集
                    InitializationInfo info = new InitializationInfo(localVar, currentUnit, method);
                    info.setInitializationValue(rightOp); // 右辺の値を初期化値として保存
                    initializations.add(info);
                }

                // リテラル値の場合
                if (rightOp instanceof IntConstant) {
                    IntConstant constant = (IntConstant) rightOp;
                    debugPrint("    Found literal value: " + constant.value, 0);
                }

                // 右辺もローカル変数の場合
                if (rightOp instanceof Local) {
                    Local localVar = (Local) rightOp;
                    debugPrint("Local variable detected on right-hand side: " + localVar.getName(), 1);

                    // 必要に応じてバックトラックを継続
                    backtrack(graph, currentUnit, visited, depth + 1, maxDepth, rightOp, method);
                }

                // 右辺を新しいターゲットとして再帰的に探索
                // backtrack(graph, currentUnit, visited, depth + 1, maxDepth, rightOp, method);
            }
        }
        if (stmt.containsInvokeExpr()) {
            debugPrint("  ::invoke stmt (c)", 1);
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            SootMethod invokedMethod = invokeExpr.getMethod();
            debugPrint("    Found method call: " + invokedMethod.getSignature(), 1);

            // 対象変数に関連する場合、メソッドを解析
            if (targetVariable == null || stmt.toString().contains(targetVariable.toString())) {
                analyzeMethod(invokedMethod, null);
            }
        }
        if (stmt instanceof ReturnStmt) {
            debugPrint("  ::return stmt", 1);
            ReturnStmt returnStmt = (ReturnStmt) stmt;
            Value returnValue = returnStmt.getOp();
            debugPrint("    Found return statement: " + returnStmt, 1);
            debugPrint("    Return value: " + returnValue, 1);

            if (returnValue instanceof Constant) {
                debugPrint("    Detected constant return value: " + returnValue, 1);
            } else if (returnValue instanceof Local) {
                // 返り値がローカル変数の場合、その代入元を探索
                backtrack(graph, returnStmt, visited, depth + 1, maxDepth, returnValue, method);
            } else {
                // 値が変数の場合、さらにバックトラッキングを試みる
                backtrack(graph, currentUnit, visited, depth + 1, maxDepth, returnValue, method);
            }
        }
        if (stmt instanceof InvokeStmt) {
            debugPrint("  ::invoke stmt", 1);
            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
            if (invokeExpr instanceof SpecialInvokeExpr) {
                SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

                // ContextThemeWrapper のコンストラクタ呼び出しを特定
                debugPrint("Detected constructor invocation: " + specialInvokeExpr, 1);

                Value base = specialInvokeExpr.getBase();
                if (base instanceof Local) {
                    Local local = (Local) base; // `local` を取得
                    debugPrint("Receiver Local found: " + local.getName(), 1);
                    debugPrint("Receiver Local type: " + local.getType().toString(), 1);
                    // 引数を解析
                    for (int i = 0; i < specialInvokeExpr.getArgCount(); i++) {
                        Value arg = specialInvokeExpr.getArg(i);
                        if (arg instanceof IntConstant) {
                            int themeValue = ((IntConstant) arg).value;
                            debugPrint("Extracted theme value from constructor: " + themeValue, 1);

                            InitializationInfo info = new InitializationInfo(local, currentUnit, method);
                            info.setInitializationValue(themeValue);
                            initializations.add(info);
                            // デバッグ出力
                            System.out.println("***test: Variable Name = " + info.getLocalName());
                            System.out.println("***test: Variable Type = " + info.getLocalTypeName());
                            System.out.println("***test: Initialization Value = " + info.getInitializationValue());
                        } else if (arg instanceof Local) {
                            Local argLocal = (Local) arg;
                            debugPrint("Argument is a Local variable: " + argLocal.getName(), 1);

                            // InitializationInfo に変数名と型を記録
                            InitializationInfo info = new InitializationInfo(local, currentUnit, method);
                            info.setInitializationValue("Variable: " + argLocal.getName() + ", Type: " + argLocal.getType().toString());
                            initializations.add(info);

                            // デバッグ出力
                            System.out.println("***test: Argument Variable Name = " + argLocal.getName());
                            System.out.println("***test: Argument Variable Type = " + argLocal.getType().toString());
                        } else {
                            debugPrint("Non-constant constructor argument: " + arg, 1);
                        }
                    }
                }
            }
        }

        // 前のユニットを探索
        for (Unit pred : graph.getPredsOf(currentUnit)) {
            backtrack(graph, pred, visited, depth + 1, maxDepth, targetVariable, method);
        }
    }

    private void analyzeMethodForFieldInitialization(SootMethod method, SootField targetField) {
        if (!method.isConcrete()) return; // 抽象メソッドや外部メソッドはスキップ

        Body body = method.retrieveActiveBody();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value leftOp = assignStmt.getLeftOp();

                // フィールドへの代入を検出
                if (leftOp instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) leftOp;
                    SootField field = fieldRef.getField();

                    if (field.equals(targetField)) {
                        debugPrint("Field " + targetField.getName() + " initialized in method: " +
                                method.getSignature() + " at statement: " + unit, 1);

                        // 初期化情報を保存
                        InitializationInfo info = new InitializationInfo(field, unit, method);
                        info.setInitializationValue(assignStmt.getRightOp()); // 初期化値を保存
                        initializations.add(info);
                    }
                }
            }

            // 再帰的にさらに呼び出されるメソッドを探索
            if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()) {
                InvokeExpr invokeExpr = ((Stmt) unit).getInvokeExpr();
                SootMethod invokedMethod = invokeExpr.getMethod();
                if (invokedMethod.isConcrete()) {
                    analyzeMethodForFieldInitialization(invokedMethod, targetField);
                }
            }
        }
    }

    // 主解析ログの出力
    public void outputAnalysisLog() {
        // 解析情報をセクションごとに出力
        printFormatter.printSection("Analysis Overview", '=');
        System.out.println("Target Variable: " + argumentName);
        System.out.println("Local Variables Mapped: " + localVariableNames.size());
        System.out.println("Field Variables Mapped: " + fieldVariableNames.size());
        System.out.println("Initialization Statements Found: " + initializations.size());

        printFormatter.printSection("Local Variable Mapping", '-');
        printFormatter.printKeyValueTable(localVariableNames, "Local Variable", "Mapped Name");

        printFormatter.printSection("Field Variable Mapping", '-');
        printFormatter.printKeyValueTable(fieldVariableNames, "Field", "Mapped Name");

        printFormatter.printSection("Local-to-Field Mapping", '-');
        printFormatter.printKeyValueTable(localToFieldMapping, "Local Value", "Mapped Field");

        // 初期化情報の詳細を出力
        outputCollectedInitializations();
    }

    // 初期化情報をJava形式で出力
    public void outputCollectedInitializations() {
        printFormatter.printSection("Formatted Initializations", '=');

        // 初期化情報をソート
        List<InitializationInfo> sortedInitializations = new ArrayList<>(initializations);
        // sortedInitializations.sort(Comparator.comparing(info -> info.getField().getName()));

        List<List<String>> tableData = new ArrayList<>();
        tableData.add(Arrays.asList("Field Type", "Field Name", "Initialized In", "Statement"));

        System.out.println("+++etts: " + sortedInitializations.size());
        // 出力
        for (InitializationInfo info : sortedInitializations) {
            String scopeName = "";
            String scopeType = "";
            if (info.getField() != null) {
                scopeName = info.getFieldName();
                scopeType = info.getFieldTypeName();
            } else if (info.getLocal() != null) {
                scopeName = info.getLocalName();
                scopeType = info.getLocalTypeName();
            }
            String methodName = info.getMethodName();

            System.out.println(scopeType + " " + scopeName + ";");
            System.out.println("// Initialized in method: " + methodName);
            System.out.println(formatStatement(scopeName, info));
        }

        // 戻り値初期化の出力
        int variableIndex = 1;
        for (Map.Entry<String, Object> entry : returnValueMap.entrySet()) {
            String methodName = entry.getKey();
            Object returnValue = entry.getValue();
            String variableName = "var" + variableIndex + "_" + methodName;

            System.out.println("int " + variableName + ";");
            System.out.println("// initialized with the return value at " + methodName);
            System.out.println(variableName + " = " + returnValue + ";");

            variableIndex++;
        }
        printFormatter.printSection("", '=');
    }

    private String formatStatement(String fieldName, InitializationInfo info) {
        Unit unit = info.getUnit();

        if (unit == null) {
            return fieldName + " = " + info.getInitializationValue() + ";";
        }

        if (unit instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) unit;
            Value rhs = assignStmt.getRightOp();
            return fieldName + " = " + rhs.toString() + ";";
        } else if (unit instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt) unit;
            return fieldName + " = " + returnStmt.getOp().toString() + ";";
        }
        return null;
    }

    private void debugPrint(String message, int debugLevel) {
        if (debugLevel <= maxDebugLevel) {
            System.out.println(message);
        }
    }
}
