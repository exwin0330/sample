package com.example.sample.flow;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.*;
import soot.options.Options;

import java.io.File;
import java.util.*;

public class CallGraphAnalysis {
    public CallGraphAnalysis(String mainClass) {
        setupSoot(mainClass);

        // メインメソッドから開始
        SootClass sootClass = Scene.v().getSootClass(mainClass);
        SootMethod entryMethod = sootClass.getMethodByName("onCreate"); // 起点となるメソッド

        // CallGraphを取得
        CallGraph callGraph = Scene.v().getCallGraph();

        // フィールド`theme`に関連する操作を追跡
        System.out.println("Analyzing field: theme");
        analyzeFieldUsage(callGraph, entryMethod, "theme");
    }

    public static void main(String[] args) {
        CallGraphAnalysis cg = new CallGraphAnalysis("getDrawableSample");
    }

    /**
     * Sootの基本設定を行うメソッド
     */
    private void setupSoot(String mainClass) {
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
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));
        Options.v().set_android_jars("C:/Users/ereve/AppData/Local/Android/Sdk/platforms/android-29/android.jar");
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Scene.v().addBasicClass(mainClass, SootClass.SIGNATURES);
        Scene.v().loadBasicClasses();
        Scene.v().loadNecessaryClasses();
    }

    /**
     * CallGraphを使用して、指定されたフィールドに関連する操作を追跡するメソッド
     */
    private void analyzeFieldUsage(CallGraph callGraph, SootMethod entryMethod, String fieldName) {
        Queue<SootMethod> worklist = new LinkedList<>();
        Set<SootMethod> visited = new HashSet<>();

        worklist.add(entryMethod);

        while (!worklist.isEmpty()) {
            SootMethod method = worklist.poll();

            if (!visited.add(method)) {
                continue; // 既に解析済み
            }

            System.out.println("Analyzing method: " + method.getSignature());

            if (!method.hasActiveBody()) {
                continue;
            }

            // メソッド内のフィールド操作を解析
            analyzeFieldInMethod(method, fieldName);

            // 次の呼び出しを探索
            Iterator<Edge> edges = callGraph.edgesOutOf(method);
            while (edges.hasNext()) {
                SootMethod target = edges.next().tgt();
                worklist.add(target);
            }
        }
    }

    /**
     * 指定されたメソッド内で、フィールド`fieldName`に関連する操作を解析するメソッド
     */
    private void analyzeFieldInMethod(SootMethod method, String fieldName) {
        Body body = method.getActiveBody();

        for (Unit unit : body.getUnits()) {
            if (unit instanceof AssignStmt) {
                AssignStmt assign = (AssignStmt) unit;
                if (assign.containsFieldRef()) {
                    SootField field = assign.getFieldRef().getField();
                    if (field.getName().equals(fieldName)) {
                        System.out.println("Field accessed in: " + method.getSignature());
                        System.out.println("  Statement: " + unit);
                    }
                }
            }
        }
    }
}
