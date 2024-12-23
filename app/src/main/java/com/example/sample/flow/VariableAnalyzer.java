package com.example.sample.flow;

import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.io.File;
import java.util.*;

public class VariableAnalyzer {
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
    public VariableAnalyzer(String className) {
        setupSoot();

        SootClass sc = Scene.v().loadClassAndSupport(className);
        sc.setApplicationClass();
    }

    private void analyze() {
        // アプリケーションクラスを走査
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            System.out.println("Analyzing class: " + sootClass.getName());

            for (SootMethod method : sootClass.getMethods()) {
                if (!method.isConcrete()) continue; // 実装のないメソッドはスキップ
                System.out.println("  Method: " + method.getName());

                Body body = method.retrieveActiveBody();
                for (Unit unit : body.getUnits()) {
                    // 行番号を取得
                    int lineNumber = getLineNumber(unit);
                    System.out.println("    Statement (Line " + lineNumber + "): " + unit);

                    List<Value> variables = extractVariables(unit);
                    if (variables.isEmpty()) {
                        System.out.println("      No variables found.");
                    } else {
                        System.out.println("      Variables:");
                        for (Value var : variables) {
                            System.out.println("        " + var + " (Type: " + var.getType() + ")");
                        }
                    }
                }
            }
        }
    }

    /**
     * ステートメントの行番号を取得
     */
    private static int getLineNumber(Unit unit) {
        // UnitにLineNumberTagがあるか確認
        for (Tag tag : unit.getTags()) {
            if (tag instanceof LineNumberTag) {
                return ((LineNumberTag) tag).getLineNumber();
            }
        }
        // 行番号が見つからない場合は-1を返す
        return -1;
    }

    /**
     * ステートメントから変数を抽出
     */
    private List<Value> extractVariables(Unit unit) {
        List<Value> variables = new ArrayList<>();

        if (unit instanceof AssignStmt) {
            // 代入文の場合、左辺と右辺を抽出
            AssignStmt assignStmt = (AssignStmt) unit;
            variables.add(assignStmt.getLeftOp());
            variables.add(assignStmt.getRightOp());
        } else if (unit instanceof InvokeStmt) {
            // メソッド呼び出し文の場合、使用される変数を抽出
            InvokeStmt invokeStmt = (InvokeStmt) unit;
            variables.addAll(extractFromInvokeExpr(invokeStmt.getInvokeExpr()));
        } else if (unit instanceof DefinitionStmt) {
            // 定義文の場合、左辺と右辺を抽出
            DefinitionStmt defStmt = (DefinitionStmt) unit;
            variables.add(defStmt.getLeftOp());
            variables.add(defStmt.getRightOp());
        } else if (unit instanceof IfStmt) {
            // 条件分岐の場合、条件式を抽出
            IfStmt ifStmt = (IfStmt) unit;
            variables.add(ifStmt.getCondition());
        }

        // 値がValueBoxに格納されている場合もあるので抽出
        for (ValueBox box : unit.getUseAndDefBoxes()) {
            variables.add(box.getValue());
        }

        // 重複を削除して返す
        return new ArrayList<>(new HashSet<>(variables));
    }

    /**
     * メソッド呼び出し式から変数を抽出
     */
    private List<Value> extractFromInvokeExpr(InvokeExpr expr) {
        List<Value> variables = new ArrayList<>();
        if (expr.getUseBoxes() != null) {
            for (ValueBox box : expr.getUseBoxes()) {
                variables.add(box.getValue());
            }
        }
        return variables;
    }

    public static void main(String[] args) {
        VariableAnalyzer va = new VariableAnalyzer("getDrawableSample");
        va.analyze();
    }
}
