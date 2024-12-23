package com.example.sample.flow.map;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // 1. Soot解析
        System.out.println("=== Soot Analysis ===");
        SootAnalysis sootAnalysis = new SootAnalysis();
        ExceptionalUnitGraph sootGraph = getDummyExceptionalUnitGraph(); // ダミーのグラフを取得
        sootAnalysis.analyzeMethod(sootGraph);

        // 取得したSoot変数マップを表示
        Map<String, String> sootVariableMap = sootAnalysis.getSootVariableMap();
        sootVariableMap.forEach((key, value) ->
                System.out.println("Soot Variable: " + key + " -> " + value));

        // 2. AST解析
        System.out.println("\n=== AST Analysis ===");
        TreeContext context = new TreeContext();

        // サンプルASTノードの生成
        Tree rootNode = context.createTree(TypeSet.type("RootType"), "rootLabel");
        Tree childNode = context.createTree(TypeSet.type("ChildType"), "childLabel");
        rootNode.addChild(childNode);

        // ASTノードをカスタム出力
        printTree(rootNode, 0);

        // ダミーのAST変数マップを生成
        Map<String, String> astVariableMap = new HashMap<>();
        astVariableMap.put("rootLabel", "AST_Root");
        astVariableMap.put("childLabel", "AST_Child");

        // 3. Soot変数とAST変数のマッピング
        System.out.println("\n=== Variable Mapping ===");
        Map<String, String> variableMapping = VariableMapper.mapVariables(
                sootVariableMap,
                astVariableMap
        );

        // マッピング結果の表示
        variableMapping.forEach((sootVar, astVar) ->
                System.out.println("Soot Variable: " + sootVar + " -> AST Variable: " + astVar));
    }

    // ダミーのExceptionalUnitGraphを生成するメソッド（実際のデータが用意できない場合に使用）
    private static ExceptionalUnitGraph getDummyExceptionalUnitGraph() {
        // 必要に応じて、ダミーまたはモックのグラフを返すよう実装する
        return null;
    }

    // ツリーを表示するためのヘルパーメソッド
    public static void printTree(Tree node, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + node.getLabel());
        for (Tree child : node.getChildren()) {
            printTree(child, depth + 1);
        }
    }
}
