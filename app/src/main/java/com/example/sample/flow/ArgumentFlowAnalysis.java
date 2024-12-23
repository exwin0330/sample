package com.example.sample.flow;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;

import java.util.HashSet;
import java.util.Set;

public class ArgumentFlowAnalysis extends BackwardFlowAnalysis<Unit, Set<Value>> {

    private final Value targetValue;

    public ArgumentFlowAnalysis(ExceptionalUnitGraph graph, Value targetValue) {
        super(graph);
        this.targetValue = targetValue;
        // フロー解析の実行
        doAnalysis();
    }

    @Override
    protected void flowThrough(Set<Value> in, Unit unit, Set<Value> out) {
        // 現在のフローをそのまま伝播
        out.clear();
        out.addAll(in);

        // ステートメントが対象変数を使用または定義しているか確認
        for (ValueBox vb : unit.getUseAndDefBoxes()) {
            if (vb.getValue().equals(targetValue)) {
                System.out.println("Relevant statement found: " + unit);
                out.add(vb.getValue());
            }
        }
    }

    @Override
    protected Set<Value> newInitialFlow() {
        return new HashSet<>();
    }

    @Override
    protected Set<Value> entryInitialFlow() {
        return new HashSet<>();
    }

    @Override
    protected void merge(Set<Value> in1, Set<Value> in2, Set<Value> out) {
        out.addAll(in1);
        out.addAll(in2);
    }

    @Override
    protected void copy(Set<Value> source, Set<Value> dest) {
        dest.clear();
        dest.addAll(source);
    }
}
