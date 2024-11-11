package com.example.sample.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GraphNode {
    private String label;
    private List<GraphEdge> edges = new ArrayList<>();

    public GraphNode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void addEdge(GraphEdge edge) {
        edges.add(edge);
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }
}

class GraphEdge {
    private GraphNode targetNode;
    private String relation;

    public GraphEdge(GraphNode targetNode, String relation) {
        this.targetNode = targetNode;
        this.relation = relation;
    }

    public GraphNode getTargetNode() {
        return targetNode;
    }

    public String getRelation() {
        return relation;
    }
}

class AnalysisGraph {
    private Map<String, GraphNode> nodes = new HashMap<>();

    public GraphNode getOrCreateNode(String label) {
        return nodes.computeIfAbsent(label, k -> new GraphNode(label));
    }

    public void addEdge(String fromLabel, String toLabel, String relation) {
        GraphNode fromNode = getOrCreateNode(fromLabel);
        GraphNode toNode = getOrCreateNode(toLabel);
        fromNode.addEdge(new GraphEdge(toNode, relation));
    }

    public void printGraph() {
        for (GraphNode node : nodes.values()) {
            System.out.println("Node: " + node.getLabel());
            for (GraphEdge edge : node.getEdges()) {
                System.out.println("  -> (" + edge.getRelation() + ") " + edge.getTargetNode().getLabel());
            }
        }
    }

    // 新規追加: 特定のメソッドとその引数のトレース出力
    public void printMethodArgumentsTrace(String methodLabel) {
        GraphNode startNode = nodes.get("Method: " + methodLabel);
        if (startNode == null) {
            System.out.println("Method not found: " + methodLabel);
            return;
        }
        System.out.println("Tracing arguments for Method: " + methodLabel);
        traceArguments(startNode, new HashSet<>());
    }

    // 引数を再帰的に辿って出力するヘルパーメソッド
    private void traceArguments(GraphNode node, Set<GraphNode> visitedNodes) {
        if (visitedNodes.contains(node)) return;
        visitedNodes.add(node);

        System.out.println("Node: " + node.getLabel());
        for (GraphEdge edge : node.getEdges()) {
            if ("defined-by".equals(edge.getRelation())) {
                System.out.println("  -> (" + edge.getRelation() + ") " + edge.getTargetNode().getLabel());
                traceArguments(edge.getTargetNode(), visitedNodes);
            }
        }
    }
}
