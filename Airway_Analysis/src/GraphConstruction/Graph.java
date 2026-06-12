package GraphConstruction;

import DataStructure.*;
import java.util.Collections;
import DataStructure.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    private final List<AirwayNode> nodes;
    private final List<AirwayEdge> edges;
    private final Map<AirwayNode, List<AirwayEdge>> adjacencyList;
    private final List<Double> width;
    

    public Graph() {
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.adjacencyList = new HashMap<>();
        this.width = new ArrayList<>();
    }

    public void addNode(AirwayNode node) {
        if (!containsNode(node)) {
            nodes.add(node);
            adjacencyList.put(node, new ArrayList<>());
        }
    }

    public void addEdge(AirwayEdge edge) {
        AirwayNode start = edge.getFrom();
        AirwayNode end = edge.getTo();

        if (!containsNode(start)) {
            addNode(start);
        }

        if (!containsNode(end)) {
            addNode(end);
        }

        if (!containsEdge(edge)) {
            edges.add(edge);
            adjacencyList.get(start).add(edge);
            adjacencyList.get(end).add(edge);

            start.addEdge(edge);
            end.addEdge(edge);
        }
    }

    public List<AirwayNode> getNodes() {
        return nodes;
    }

    public List<AirwayEdge> getEdges() {
        return edges;
    }

    public List<AirwayEdge> getConnectedEdges(AirwayNode node) {
        return adjacencyList.containsKey(node)
                ? adjacencyList.get(node)
                : Collections.emptyList();
    }

    public boolean containsNode(AirwayNode node) {
        return nodes.contains(node);
    }

    public boolean containsEdge(AirwayEdge edge) {
        return edges.contains(edge);
    }

    public void printGraph() {
        for (AirwayNode node : nodes) {
            System.out.println("Node: " + node);

            List<AirwayEdge> connectedEdges = adjacencyList.get(node);
            if (connectedEdges != null) {
                for (AirwayEdge edge : connectedEdges) {
                    System.out.println("   " + edge);
                }
            }
        }
    }
}