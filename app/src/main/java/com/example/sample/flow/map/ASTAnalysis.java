package com.example.sample.flow.map;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

public class ASTAnalysis {
    public static void main(String[] args) {
        // TreeContextの初期化
        TreeContext context = new TreeContext();

        // TypeSet.typeでTypeを生成
        Tree rootNode = context.createTree(TypeSet.type("RootType"), "rootLabel");
        Tree childNode = context.createTree(TypeSet.type("ChildType"), "childLabel");

        // ノードの追加
        rootNode.addChild(childNode);

        // ノード情報の出力
        System.out.println("Root Node Label: " + rootNode.getLabel());
        System.out.println("Child Node Label: " + childNode.getLabel());

        // ツリー構造をカスタム出力
        printTree(rootNode, 0);
    }

    // ツリーを表示するためのヘルパーメソッド
    public static void printTree(Tree node, int depth) {
        // 深さに応じてインデントを追加
        String indent = "  ".repeat(depth);
        System.out.println(indent + node.getLabel());

        for (Tree child : node.getChildren()) {
            printTree(child, depth + 1);
        }
    }
}
