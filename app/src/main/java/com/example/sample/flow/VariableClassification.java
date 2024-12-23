package com.example.sample.flow;

public class VariableClassification {
    public enum Type {
        INITIALIZED, // メソッド内で初期化
        ARGUMENT,    // メソッドの引数
        FIELD,
        OTHER        // それ以外（フィールドなど）
    }

    private final String variableName;
    private final Type type;

    public VariableClassification(String variableName, Type type) {
        this.variableName = variableName;
        this.type = type;
    }

    public String getVariableName() {
        return variableName;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Variable '" + variableName + "' classified as: " + type;
    }
}
