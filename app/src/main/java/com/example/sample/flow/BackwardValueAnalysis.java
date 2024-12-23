package com.example.sample.flow;

import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;

public class BackwardValueAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<Value>> {

    private final Value target;

    public BackwardValueAnalysis(UnitGraph graph, Value target) {
        super(graph);
        this.target = target;
    }

    @Override
    protected FlowSet<Value> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected FlowSet<Value> entryInitialFlow() {
        FlowSet<Value> set = new ArraySparseSet<>();
        set.add(target); // 初期フローに解析対象変数を追加
        return set;
    }

    @Override
    protected void flowThrough(FlowSet<Value> in, Unit unit, FlowSet<Value> out) {
        in.copy(out);

        if (unit instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) unit;

            // 左辺の変数が入力フローに含まれている場合
            if (in.contains(stmt.getLeftOp())) {
                System.out.println("  Variable defined at: " + unit);
                out.add(stmt.getRightOp()); // 右辺の変数をフローに追加
            }
        }
    }

    @Override
    protected void merge(FlowSet<Value> in1, FlowSet<Value> in2, FlowSet<Value> out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(FlowSet<Value> source, FlowSet<Value> dest) {
        source.copy(dest);
    }

    // 外部から呼び出せるメソッド
    public void runAnalysis() {
        doAnalysis(); // protected メソッドを内部で呼び出す
    }
}
