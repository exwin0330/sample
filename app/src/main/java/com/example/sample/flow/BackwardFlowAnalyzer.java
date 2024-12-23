package com.example.sample.flow;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.*;
import soot.options.Options;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;

import java.io.File;
import java.util.*;

public class BackwardFlowAnalyzer {

    public static void main(String[] args) {
        // Soot の初期設定
        initializeSoot();

        // 対象クラスとメソッドの指定
        String targetMethod = "<getDrawableSample: void getThemeResource()>";
        BackwardFlowAnalyzer analyzer = new BackwardFlowAnalyzer();

        // メソッドを使用しているステートメントを解析
        analyzer.analyzeMethodUsage(targetMethod);
    }

    /**
     * Soot の初期化
     */
    public static void initializeSoot() {
        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(Collections.singletonList("path/to/your/classes")); // クラスファイルのパス
        Options.v().setPhaseOption("cg.spark", "on");
        Scene.v().loadNecessaryClasses();
    }

    /**
     * 指定されたメソッドを使用しているステートメントを探索
     */
    public void analyzeMethodUsage(String targetMethodSignature) {
        // コールグラフの取得
        CallGraph cg = Scene.v().getCallGraph();

        // すべてのメソッドを走査
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            for (SootMethod method : clazz.getMethods()) {
                if (!method.isConcrete()) continue;

                Body body = method.retrieveActiveBody();
                for (Unit unit : body.getUnits()) {
                    if (unit instanceof InvokeStmt || unit instanceof AssignStmt) {
                        InvokeExpr invokeExpr = getInvokeExpr(unit);
                        if (invokeExpr != null && invokeExpr.getMethod().getSignature().equals(targetMethodSignature)) {
                            System.out.println("Found invocation in method: " + method.getSignature());
                            System.out.println("  Statement: " + unit);

                            // メソッド呼び出しの引数を解析
                            List<Value> args = invokeExpr.getArgs();
                            for (Value arg : args) {
                                System.out.println("  Argument: " + arg);
                                analyzeArgumentFlow(arg, body, unit);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 引数のデータフロー解析（後方スライス）
     */
    private void analyzeArgumentFlow(Value arg, Body body, Unit startUnit) {
        // グラフを作成
        UnitGraph graph = new BriefUnitGraph(body);

        // カスタムクラスを使って解析を実行
        BackwardValueAnalysis analysis = new BackwardValueAnalysis(graph, arg);
        analysis.doAnalysis();

        // 解析結果の出力
        for (Unit unit : graph) {
            FlowSet<Value> flowSet = analysis.getFlowBefore(unit);
            if (!flowSet.isEmpty()) {
                System.out.println("  Possible definitions before: " + unit);
                System.out.println("    " + flowSet);
            }
        }
    }


    /**
     * ステートメントから InvokeExpr を取得
     */
    private InvokeExpr getInvokeExpr(Unit unit) {
        if (unit instanceof InvokeStmt) {
            return ((InvokeStmt) unit).getInvokeExpr();
        } else if (unit instanceof AssignStmt) {
            Value rightOp = ((AssignStmt) unit).getRightOp();
            if (rightOp instanceof InvokeExpr) {
                return (InvokeExpr) rightOp;
            }
        }
        return null;
    }
}

