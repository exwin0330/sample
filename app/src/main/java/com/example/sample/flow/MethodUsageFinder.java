package com.example.sample.flow;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.options.Options;

import java.io.File;
import java.util.Collections;

public class MethodUsageFinder {
    private String className;
    private String targetMethodName;

    public MethodUsageFinder(String className, String targetMethodName) {
        initializeSoot(className);
        this.className = className;
        this.targetMethodName = targetMethodName;
    }

    public void findMethodUsages() {
        // 対象クラスをロード
        SootClass clazz = Scene.v().getSootClass(className);

        System.out.println("Searching for usages of method: " + targetMethodName);

        // クラス内の各メソッドを解析
        for (SootMethod method : clazz.getMethods()) {
            if (!method.isConcrete()) continue;

            System.out.println("Analyzing method: " + method.getSignature());
            analyzeMethodForUsages(method);
        }
    }

    private void analyzeMethodForUsages(SootMethod method) {
        // メソッドボディを取得
        Body body = method.retrieveActiveBody();

        // 各ユニット（ステートメント）をチェック
        for (Unit unit : body.getUnits()) {
            if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                // ターゲットメソッド名と一致する呼び出しを探す
                if (invokeExpr.getMethod().getName().equals(targetMethodName)) {
                    System.out.println("Found usage in method: " + method.getSignature());
                    System.out.println("  Usage: " + unit);
                }
            }
        }
    }

    public static void initializeSoot(String className) {
        String sourceDirectory = System.getProperty("user.dir") +
                File.separator + "build" +
                File.separator + "intermediates" +
                File.separator + "javac" +
                File.separator + "debug" +
                File.separator + "classes";

        String androidJarPath = "C:/Users/ereve/AppData/Local/Android/Sdk/platforms/android-29/android.jar";
        String sootPath = "libs/soot-4.5.0-jar-with-dependencies.jar";

        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));

        Options.v().set_android_jars(androidJarPath);
        Options.v().set_soot_classpath(
                sourceDirectory + File.pathSeparator +
                        androidJarPath + File.pathSeparator +
                        sootPath
        );
        Options.v().set_allow_phantom_refs(true);

        Scene.v().addBasicClass(className, SootClass.BODIES);
        Scene.v().loadNecessaryClasses();
    }

    public static void main(String[] args) {
        // サンプルとしてgetDrawableを探す
        MethodUsageFinder finder = new MethodUsageFinder("com.example.sample.flow.files.getDrawableSample", "getDrawable");
        finder.findMethodUsages();
    }
}
