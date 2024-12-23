package com.example.sample.flow;

import soot.*;
import soot.jimple.toolkits.callgraph.*;
import soot.jimple.*;
import soot.options.Options;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

public class SootLinkAnalyzer {

    public static void main(String[] args) {
        // Soot 初期化設定
        initializeSoot("getDrawableSample");

        // 解析クラスの指定
        SootClass clazz = Scene.v().getSootClass("getDrawableSample");

        // インスタンス化して解析を実行
        SootLinkAnalyzer analyzer = new SootLinkAnalyzer();
        analyzer.analyzeClass(clazz, "theme");
    }

    /**
     * Soot の初期化
     */
    public static void initializeSoot(String className) {
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
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(java.util.Collections.singletonList(sourceDirectory)); // クラスファイルのパス
        Options.v().setPhaseOption("cg.spark", "on");
        Scene.v().addBasicClass(className, SootClass.BODIES);

        Scene.v().loadNecessaryClasses();

        // 特定のメソッドをエントリーポイントに指定
        setSpecificEntryPoint(className, "onCreate");
    }

    public static void setSpecificEntryPoint(String className, String methodName) {
        SootClass clazz = Scene.v().getSootClass(className);

        // 指定されたメソッドを取得
        SootMethod method = clazz.getMethodByName(methodName);

        // エントリーポイントとして設定
        Scene.v().setEntryPoints(Collections.singletonList(method));

        // CallGraph を作成
        PackManager.v().getPack("cg").apply();
    }

    /**
     * クラス全体の解析
     */
    public void analyzeClass(SootClass clazz, String fieldName) {
        System.out.println("Analyzing class: " + clazz.getName());

        // フィールドアクセスの解析
        analyzeFieldAccess(clazz, fieldName);

        // メソッド間リンクの解析
        analyzeMethodLinks(clazz);
    }

    /**
     * フィールドアクセスの解析
     */
    public void analyzeFieldAccess(SootClass clazz, String fieldName) {
        System.out.println("Field Access Analysis for: " + fieldName);

        SootField field = clazz.getFieldByName(fieldName);

        for (SootMethod method : clazz.getMethods()) {
            if (!method.isConcrete()) continue;

            Body body = method.retrieveActiveBody();
            for (Unit unit : body.getUnits()) {
                if (unit.toString().contains(field.getSignature())) {
                    System.out.println("  Field " + fieldName + " accessed in method: " + method.getName());
                    System.out.println("    Statement: " + unit);
                }
            }
        }
    }

    /**
     * メソッド間リンクの解析
     */
    public void analyzeMethodLinks(SootClass clazz) {
        System.out.println("Method Link Analysis:");

        CallGraph cg = Scene.v().getCallGraph();

        for (SootMethod method : clazz.getMethods()) {
            if (!method.isConcrete()) continue;

            System.out.println("Analyzing method: " + method.getSignature());

            Iterator<Edge> edges = cg.edgesOutOf(method);
            while (edges.hasNext()) {
                Edge edge = edges.next();
                SootMethod target = edge.getTgt().method();

                System.out.println("  Calls: " + target.getSignature());
                analyzeMethodFlow(method, target);
            }
        }
    }

    /**
     * メソッド間のデータフロー解析
     */
    private void analyzeMethodFlow(SootMethod source, SootMethod target) {
        try {
            System.out.println("Analyzing data flow from " + source.getName() + " to " + target.getSignature());

            // ソースメソッドの Body を取得
            Body sourceBody = source.retrieveActiveBody();

            // ターゲットメソッドの Body を取得（存在しない場合もある）
            if (!target.hasActiveBody()) {
                System.out.println("  Target method " + target.getName() + " has no active body.");
                return;
            }

            Body targetBody = target.retrieveActiveBody();

            // データフローの解析（例: InvokeStmt を解析）
            for (Unit unit : sourceBody.getUnits()) {
                if (unit instanceof InvokeStmt) {
                    InvokeExpr invokeExpr = ((InvokeStmt) unit).getInvokeExpr();
                    if (invokeExpr.getMethod().equals(target)) {
                        System.out.println("    Link found at: " + unit);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("    Exception while analyzing flow: " + e.getMessage());
        }
    }
}
