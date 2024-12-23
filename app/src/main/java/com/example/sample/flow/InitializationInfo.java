package com.example.sample.flow;

import java.util.Objects;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;

// 初期化情報を格納するクラス
public class InitializationInfo {
    private final SootField field;
    private final Local local;
    private Unit unit;
    private SootMethod method;

    private Object initializationValue;

    public InitializationInfo(SootField field, Unit unit, SootMethod method) {
        this.field = field;
        this.local = null;
        this.unit = unit;
        this.method = method;
        this.initializationValue = null;
    }

    public InitializationInfo(Local local, Unit unit, SootMethod method) {
        this.field = null;
        this.local = local;
        this.unit = unit;
        this.method = method;
        this.initializationValue = null;
    }

    public InitializationInfo(Unit unit, SootMethod method) {
        this.field = null;
        this.local = null;
        this.unit = unit;
        this.method = method;
        this.initializationValue = null;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public void setMethod(SootMethod method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitializationInfo that = (InitializationInfo) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(local, that.local) &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(method, that.method);
    }

    @Override
    public String toString() {
        return "Field: " + field.getName() +
                ", Statement: " + unit.toString() +
                ", Method: " + method.getSignature();
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, unit, method);
    }

    public SootField getField() {
        return field;
    }

    public Local getLocal() { return local; }

    public Unit getUnit() {
        return unit;
    }

    public SootMethod getMethod() {
        return method;
    }

    // 初期化時の値をセットする
    public void setInitializationValue(Object value) {
        this.initializationValue = value;
    }

    // 初期化時の値を取得する
    public Object getInitializationValue() {
        return initializationValue;
    }

    public String getFieldName() {
        return field != null ? field.getName() : "Unknown Field";
    }

    public String getLocalName() {
        return local != null ? local.getName() : "Unknown Local";
    }

    public String getFieldTypeName() {
        return field != null ? field.getType().toString() : "Unknown Field Type";
    }

    public String getLocalTypeName() {
        return local != null ? local.getType().toString() : "Unknown Local Type";
    }

    public String getMethodName() {
        return method != null ? method.getName() : "Unknown Method";
    }
}
