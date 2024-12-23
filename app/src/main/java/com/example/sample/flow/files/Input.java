package com.example.sample.flow.files;

public class Input {
    private String className;
    private String argumentName;

    public Input(String className, String argumentName) {
        this.className = className;
        this.argumentName = argumentName;
    }

    public String getClassName() {
        return className;
    }

    public String getArgumentName() {
        return argumentName;
    }
}
