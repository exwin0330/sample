package com.example.sample.flow;

import com.example.sample.flow.files.Input;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReverseFlowAnalyzer {
    private final String className;
    private final String APIMethodName;

    private final String variableName;
    private final Set<SootMethod> visitedMethods = new HashSet<>(); // 訪問済みメソッドを記録

    public ReverseFlowAnalyzer(String className, String APIMethodName, String variableName) {
        SootInitializer.initialize(className);
        this.className = className;
        this.APIMethodName = APIMethodName;
        this.variableName = variableName;
    }

    public void analyze() {
        SootClass targetClass = Scene.v().getSootClass(className);
        // BacktrackingResult results = new BacktrackingResult();
        // MethodAnalyzerOld analyzer = new MethodAnalyzerOld(results);
        // analyzer.analyzeClass(targetClass, APIMethodName);

        MethodAnalyzer analyzer = new MethodAnalyzer(variableName);
        analyzer.analyzeClassWithMethodName(targetClass, APIMethodName, "onCreate");
    }

    public static void main(String[] args) {
        String header = "com.example.sample.flow.files.";

        List<Input> inputList = new ArrayList<>();
        inputList.add(new Input("getDrawableSampleEasyS", "theme"));
        //inputList.add(new Input("getDrawableSampleEasy", "theme"));
        //inputList.add(new Input("getDrawableSampleNormal", "theme"));
        //inputList.add(new Input("getDrawableSample", "theme"));
        //inputList.add(new Input("getDrawableSampleHard", "getID"));

        for (Input input : inputList) {
            System.out.println("\n=== " + input.getClassName() + " ===\n");
            ReverseFlowAnalyzer analyzer = new ReverseFlowAnalyzer(header + input.getClassName(), "getDrawable", input.getArgumentName());
            analyzer.analyze();
        }
    }
}
