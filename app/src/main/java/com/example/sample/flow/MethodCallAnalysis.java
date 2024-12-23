package com.example.sample.flow;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.Constant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.util.Chain;

public class MethodCallAnalysis {
    private AnalysisGraph graph = new AnalysisGraph();
    private Set<String> visitedMethods = new HashSet<>();
    private Queue<String> methodQueue = new LinkedList<>();

    private ExceptionalUnitGraph cfg;
    private SimpleLocalDefs localDefs;

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
                // String methodNodeLabel = "Method: " + method.getName();
                // graph.getOrCreateNode(methodNodeLabel);
                cfg = new ExceptionalUnitGraph(body);
                localDefs = new SimpleLocalDefs(cfg);

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
                                traceDefinition((Local) arg, unit, 0);
                            } else if (arg instanceof InstanceFieldRef) {
                                // フィールド参照が引数の場合、そのフィールドの定義箇所を追跡
                                SootField field = ((InstanceFieldRef) arg).getField();
                                traceFieldDefinition(field, method);
                            }
                        }

                        // 呼び出されたメソッドをキューに追加
                        if (!visitedMethods.contains(method.getName()) && !methodQueue.contains(method.getName())) {
                            methodQueue.add(method.getName());
                        }

                        // メソッドの引数を出力する際に、printValueTree を修正して渡す
                        for (Value arg : invokeExpr.getArgs()) {
                            printValueTree(arg, 1, localDefs, unit);
                        }
                    }
                }
            }

            private void printValueTree(Value value, int depth, SimpleLocalDefs localDefs, Unit unit) {
                // インデントを生成
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < depth; i++) {
                    builder.append("  ");
                }
                String indent = builder.toString();

                // 値の基本情報を表示
                System.out.println(indent + "Value: " + value + " (Type: " + value.getType() + ")");

                if (value instanceof Local) {
                    Local localVar = (Local) value;
                    // Local変数の名前を取得して表示
                    System.out.println(indent + "Local Variable Name: " + localVar.getName());
                    traceDefinition(localVar, unit, depth + 1);
                } else if (value instanceof InvokeExpr) {
                    // メソッド呼び出しの場合、メソッド名と引数を出力
                    InvokeExpr invokeExpr = (InvokeExpr) value;
                    System.out.println(indent + "Invoke Method: " + invokeExpr.getMethod().getName());

                    if (invokeExpr.getArgs().isEmpty()) {
                        System.out.println(indent + "No arguments found, tracing definitions...");
                        for (Value arg : invokeExpr.getArgs()) {
                            if (arg instanceof Local) {
                                traceDefinition((Local) arg, unit, depth + 1);
                            } else {
                                printValueTree(arg, depth + 1, localDefs, unit);
                            }
                        }
                    } else {
                        // 引数を再帰的に表示
                        for (Value arg : invokeExpr.getArgs()) {
                            printValueTree(arg, depth + 1, localDefs, unit);
                        }
                    }
                } else if (value instanceof Constant) {
                    System.out.println(indent + "Constant: " + value);
                } else if (value instanceof NewExpr) {
                    NewExpr newExpr = (NewExpr) value;
                    System.out.println(indent + "New Instance of: " + newExpr.getType());
                } else if (value instanceof BinopExpr) {
                    BinopExpr binopExpr = (BinopExpr) value;
                    System.out.println(indent + "Binary Operation: " + binopExpr.getSymbol());
                    printValueTree(binopExpr.getOp1(), depth + 1, localDefs, unit);
                    printValueTree(binopExpr.getOp2(), depth + 1, localDefs, unit);
                } else {
                    System.out.println(indent + "Unhandled Value Type: " + value.getClass().getSimpleName());
                }
            }

            private void traceDefinition(Local local, Unit unit, int depth) {
                for (Unit defUnit : localDefs.getDefsOfAt(local, unit)) {
                    System.out.println("Definition found for " + local + " in statement: " + defUnit);

                    if (defUnit instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) defUnit;
                        Value rightOp = assignStmt.getRightOp();
                        System.out.println("Assigned Value: " + rightOp + " in statement: " + assignStmt);

                        // `NewExpr`または`SpecialInvokeExpr`に応じて追跡
                        if (rightOp instanceof NewExpr) {
                            NewExpr newExpr = (NewExpr) rightOp;
                            if (newExpr.getType().toString().contains("ContextThemeWrapper")) {
                                System.out.println("ContextThemeWrapper instance creation detected in: " + defUnit);

                                // コンストラクタ引数を追跡
                                if (defUnit instanceof InvokeStmt) {
                                    InvokeStmt invokeStmt = (InvokeStmt) defUnit;
                                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                                    for (Value arg : invokeExpr.getArgs()) {
                                        System.out.println("Constructor argument for ContextThemeWrapper: " + arg);
                                        if (arg instanceof Local) {
                                            traceDefinition((Local) arg, defUnit, depth + 1); // 再帰的に追跡
                                        } else if (arg instanceof Constant) {
                                            System.out.println("Constant argument: " + arg);
                                        }
                                    }
                                }
                            }
                        } else if (rightOp instanceof InvokeExpr) {
                            InvokeExpr invokeExpr = (InvokeExpr) rightOp;
                            if (invokeExpr.getMethod().getName().equals("getTheme")) {
                                System.out.println("getTheme() method call detected.");

                                // `getTheme`の呼び出し元を再帰的に追跡
                                if (invokeExpr.getArgs().size() > 0) {
                                    for (Value arg : invokeExpr.getArgs()) {
                                        if (arg instanceof Local) {
                                            traceDefinition((Local) arg, defUnit, depth + 1);
                                        }
                                    }
                                }
                            }
                        } else if (rightOp instanceof Local) {
                            Local sourceLocal = (Local) rightOp;
                            System.out.println("Source local variable detected for further tracking: " + sourceLocal);
                            traceDefinition(sourceLocal, defUnit, depth + 1); // 深い再帰追跡
                        }
                    }
                }
            }

            // フィールド変数の定義をトレースする
            private void traceFieldDefinition(SootField field, SootMethod method) {
                Body body = method.retrieveActiveBody();
                for (Unit unit : body.getUnits()) {
                    if (unit instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) unit;
                        if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
                            InstanceFieldRef fieldRef = (InstanceFieldRef) assignStmt.getLeftOp();
                            if (fieldRef.getField().equals(field)) {
                                System.out.println("Field " + field.getName() + " defined in statement: " + assignStmt);
                                System.out.println("Assigned Value: " + assignStmt.getRightOp());
                            }
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
