package com.example.sample.flow;

import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InterproceduralFlowAnalyzer {
    protected String className;
    protected String methodName;

    public InterproceduralFlowAnalyzer(String className, String methodName) {
        // 正しいクラス名を指定してSootを初期化
        initializeSoot(className);
        this.className = className;
        this.methodName = methodName;
    }

    public void analyze() {
        // クラス確認
        SootClass clazz = Scene.v().getSootClass(className);

        /*
        // メソッド確認
        for (SootMethod method : clazz.getMethods()) {
            System.out.println("Loaded method: " + method.getSignature());
        }

        SootMethod targetMethod = null;
        // メソッド検索とデバッグ
        for (SootMethod method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                System.out.println("Found " + methodName + ": " + method.getSignature());
                targetMethod = method;
                break;
            }
        }

        if (targetMethod == null) {
            System.out.println(methodName + " method not found in class: " + clazz.getName());
            return;
        }

        analyzeMethod(targetMethod);
        */

        System.out.println("Searching for usages of method: " + methodName);

        // クラス内のすべてのメソッドを調べる
        for (SootMethod method : clazz.getMethods()) {
            if (!method.isConcrete()) continue;

            System.out.println("Analyzing method: " + method.getSignature());
            analyzeMethod(method);
        }
    }

    public static void main(String[] args) {
        InterproceduralFlowAnalyzer analyzer = new InterproceduralFlowAnalyzer("com.example.sample.flow.files.getDrawableSample", "getDrawable");
        // メソッド解析を実行
        analyzer.analyze();
    }

    public static void initializeSoot(String className) {
        String sourceDirectory = System.getProperty("user.dir") +
                File.separator + "build" +
                File.separator + "intermediates" +
                File.separator + "javac" +
                File.separator + "debug" +
                File.separator + "classes";

        String androidJarPath = "C:/Users/ereve/AppData/Local/Android/Sdk/platforms/android-29/android.jar"; // Android SDKのパスに合わせて修正
        String sootPath = "libs/soot-4.5.0-jar-with-dependencies.jar";

        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));

        // Android ライブラリをクラスパスに追加
        Options.v().set_android_jars(androidJarPath);
        Options.v().set_soot_classpath(
            sourceDirectory + File.pathSeparator +
            androidJarPath + File.pathSeparator +
            sootPath
        );
        // System.out.println("Soot classpath: " + Options.v().soot_classpath());
        Options.v().set_allow_phantom_refs(true);

        // Sparkによる呼び出し関係解析を有効化
        Options.v().setPhaseOption("cg.spark", "on");

        // クラスを追加
        Scene.v().addBasicClass(className, SootClass.BODIES);
        Scene.v().loadNecessaryClasses();
    }

    public void analyzeMethod(SootMethod method) {
        System.out.println("Analyzing method: " + method.getSignature());

        if (!method.isConcrete()) {
            System.out.println("  Method is not concrete, skipping.");
            return;
        }

        Body body = method.retrieveActiveBody();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                /*
                System.out.println("  Method call found: " + invokeExpr.getMethod().getSignature());

                // 引数の追跡
                for (Value arg : invokeExpr.getArgs()) {
                    analyzeArgumentFlow(arg, method, invokeExpr.getMethod());
                }
                */

                // ターゲットメソッド名と一致する呼び出しを探す
                if (invokeExpr.getMethod().getName().equals(methodName)) {
                    System.out.println("Found usage in method: " + method.getSignature());
                    System.out.println("  Usage: " + unit);
                    backtrackFromUnit(new ExceptionalUnitGraph(body), unit);
                }
            }
        }
    }

    private void backtrackFromUnit(ExceptionalUnitGraph graph, Unit startUnit) {
        Set<Unit> visitedUnits = new HashSet<>();
        Set<String> printedResults = new HashSet<>();
        backtrackRecursive(graph, startUnit, visitedUnits, printedResults, 0, 10);
    }

    private void backtrackRecursive(ExceptionalUnitGraph graph, Unit currentUnit, Set<Unit> visitedUnits, Set<String> printedResults, int depth, int maxDepth) {
        if (depth > maxDepth) {
            System.out.println("  Reached maximum analysis depth.");
            return;
        }

        if (visitedUnits.contains(currentUnit)) {
            return;
        }

        visitedUnits.add(currentUnit);

        // ステートメントを解析
        if (currentUnit instanceof DefinitionStmt) {
            DefinitionStmt defStmt = (DefinitionStmt) currentUnit;
            Value defValue = defStmt.getRightOp();

            String result = "  Defined at: " + currentUnit;
            if (!printedResults.contains(result)) {
                System.out.println(result);
                printedResults.add(result);
            }

            System.out.println("  Backtracking for: " + defValue);

            // 定義元が別の値を参照している場合、それを追跡
            for (ValueBox useBox : defStmt.getUseBoxes()) {
                Value usedValue = useBox.getValue();
                System.out.println("  Checking related value: " + usedValue);

                // 関連する値をバックトラック
                for (Unit predUnit : graph.getPredsOf(currentUnit)) {
                    backtrackRecursive(graph, predUnit, visitedUnits, printedResults, depth + 1, maxDepth);
                }
            }
        } else {
            // 定義でない場合も前のステートメントを追跡
            for (Unit predUnit : graph.getPredsOf(currentUnit)) {
                backtrackRecursive(graph, predUnit, visitedUnits, printedResults, depth + 1, maxDepth);
            }
        }
    }

    public void analyzeArgumentFlow(Value arg, SootMethod caller, SootMethod callee) {
        if (callee.isPhantom()) {
            System.out.println("    Skipping phantom method: " + callee.getSignature());
            return;
        }

        ExceptionalUnitGraph callerGraph = new ExceptionalUnitGraph(caller.retrieveActiveBody());
        ExceptionalUnitGraph calleeGraph = new ExceptionalUnitGraph(callee.retrieveActiveBody());

        System.out.println("  Argument: " + arg);

        Set<Value> visitedValues = new HashSet<>();
        Set<String> printedResults = new HashSet<>();
        int maxDepth = 10; // 再帰の最大深さを設定
        // 引数のフロー解析
        System.out.println("    Flow results in caller method:");
        analyzeFlowInGraph(callerGraph, arg, visitedValues, printedResults, 0, maxDepth);

        System.out.println("    Flow results in callee method:");
        analyzeFlowInGraph(calleeGraph, arg, visitedValues, printedResults, 0, maxDepth);
    }

    public void analyzeFlowInGraph(ExceptionalUnitGraph graph, Value arg, Set<Value> visitedValues, Set<String> printedResults, int depth, int maxDepth) {
        // 再帰の深さをチェック
        if (depth > maxDepth) {
            System.out.println("      Reached maximum analysis depth");
            return;
        }

        // すでに追跡した値はスキップ
        if (visitedValues.contains(arg)) {
            return;
        }

        // 現在の値を追跡済みに追加
        visitedValues.add(arg);

        for (Unit unit : graph) {
            for (ValueBox vb : unit.getUseBoxes()) {
                if (vb.getValue().equals(arg)) {
                    String result = "      Defined at: " + unit;

                    // 重複結果を省略
                    if (!printedResults.contains(result)) {
                        System.out.println(result);
                        printedResults.add(result);
                    }

                    // バックトラッキング処理
                    if (unit instanceof DefinitionStmt) {
                        Value defValue = ((DefinitionStmt) unit).getRightOp();
                        System.out.println("      Backtracking for: " + defValue);
                        analyzeFlowInGraph(graph, defValue, visitedValues, printedResults, depth + 1, maxDepth);
                    }
                }
            }
        }
    }

    // 逆追跡用関数
    private void backtrackDefinition(ExceptionalUnitGraph graph, Value targetValue, Unit startUnit) {
        for (Unit predUnit : graph.getPredsOf(startUnit)) {
            for (ValueBox vb : predUnit.getDefBoxes()) {
                if (vb.getValue().equals(targetValue)) {
                    System.out.println("        Found definition in: " + predUnit);

                    // 定義元が別の変数を参照している場合、それも追跡
                    for (ValueBox useBox : predUnit.getUseBoxes()) {
                        Value usedValue = useBox.getValue();
                        System.out.println("        Checking related value: " + usedValue);
                        backtrackDefinition(graph, usedValue, predUnit);
                    }

                    // 再帰的にさらに前の定義元を追跡
                    backtrackDefinition(graph, targetValue, predUnit);
                }
            }
        }
    }
}
