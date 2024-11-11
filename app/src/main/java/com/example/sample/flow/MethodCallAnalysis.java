package com.example.sample.flow;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

public class MethodCallAnalysis {
    private AnalysisGraph graph = new AnalysisGraph();
    private Set<String> visitedMethods = new HashSet<>();
    private Queue<String> methodQueue = new LinkedList<>();

    public MethodCallAnalysis(String className) {
        setupSoot();

        SootClass sc = Scene.v().loadClassAndSupport(className);
        sc.setApplicationClass();
    }

    private void setupSoot() {
        String sourceDirectory = System.getProperty("user.dir") +
                File.separator + "build" +
                File.separator + "intermediates" +
                File.separator + "javac" +
                File.separator + "debug" +
                File.separator + "classes" +
                File.separator + "com" +
                File.separator + "example" +
                File.separator + "sample" +
                File.separator + "flow" +
                File.separator + "files";
        Options.v().set_prepend_classpath(true);
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_app(true);
        Scene.v().loadBasicClasses();
    }

    public void analyze(String methodName) {
        methodQueue.add(methodName);

        while (!methodQueue.isEmpty()) {
            String currentMethod = methodQueue.poll();
            if (visitedMethods.contains(currentMethod)) continue;

            System.out.println("=== Analyzing method: " + currentMethod + " ===");
            visitedMethods.add(currentMethod);
            runAnalysis(currentMethod);
        }
    }

    public void runAnalysis(String targetMethodName) {
        // メソッドの呼び出しを探す
        PackManager.v().getPack("jtp").add(new Transform("jtp.myTransform", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String phase, Map<String, String> options) {
                SootMethod method = body.getMethod();
                String methodNodeLabel = "Method: " + method.getName();
                graph.getOrCreateNode(methodNodeLabel);

                BriefUnitGraph cfg = new BriefUnitGraph(body);
                SimpleLocalDefs localDefs = new SimpleLocalDefs(cfg);

                for (Unit unit : body.getUnits()) {
                    if (!(unit instanceof InvokeStmt || (unit instanceof AssignStmt && ((AssignStmt) unit).containsInvokeExpr()))) {
                        continue;
                    }

                    InvokeExpr invokeExpr = (unit instanceof InvokeStmt) ?
                            ((InvokeStmt) unit).getInvokeExpr() :
                            ((AssignStmt) unit).getInvokeExpr();

                    // 標準パッケージのメソッドを除外するフィルタリング
                    String methodClass = invokeExpr.getMethod().getDeclaringClass().getName();
                    if (methodClass.startsWith("java.") || methodClass.startsWith("javax.")) {
                        continue;
                    }


                    // ターゲットメソッドと一致するかを確認
                    if (invokeExpr.getMethod().getName().equals(targetMethodName)) {
                        System.out.println("Found target method: " + targetMethodName + " in statement: " + unit);
                        System.out.println("Within method: " + method.getName());

                        // 引数の定義箇所を追跡
                        for (Value arg : invokeExpr.getArgs()) {
                            if (arg instanceof Constant) {
                                // リテラルの定数値を出力
                                System.out.println("Constant argument found: " + arg);
                            } else if (arg instanceof Local) {
                                // 引数がローカル変数の場合、定義箇所を追跡
                                traceDefinition((Local) arg, unit, localDefs);
                            }
                        }

                        // 呼び出されたメソッドをキューに追加
                        if (!visitedMethods.contains(method.getName()) && !methodQueue.contains(method.getName())) {
                            methodQueue.add(method.getName());
                        }
                    }
                }
            }

            private void traceDefinition(Local local, Unit unit, SimpleLocalDefs localDefs) {
                for (Unit defUnit : localDefs.getDefsOfAt(local, unit)) {
                    System.out.println("Definition found for " + local + " in statement: " + defUnit);

                    if (defUnit instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) defUnit;
                        Value rightOp = assignStmt.getRightOp();

                        if (rightOp instanceof Local) {
                            traceDefinition((Local) rightOp, defUnit, localDefs);
                        }
                    }
                }
            }
        }));

        PackManager.v().runPacks();
        PackManager.v().getPack("jtp").remove("jtp.myTransform");

        // graph.printGraph();
    }

    public static void main(String[] args) {
        MethodCallAnalysis methodCallAnalysis = new MethodCallAnalysis("getDrawableSample");
        methodCallAnalysis.analyze("getDrawable");

        // methodCallAnalysis.graph.printMethodArgumentsTrace("add");
    }
}
