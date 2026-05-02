package PathFindingDectection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import DataStructure.HeapNode;
import DataStructure.MinHeap;
import GraphConstruction.AirwayEdge;
import GraphConstruction.AirwayNode;
import GraphConstruction.Graph;
import GraphConstruction.NodeType;

public class PathFind {
    private Graph graph;
    private MinHeap<AirwayNode> heap;

    public PathFind(Graph graph) {
        this.graph = graph;
        this.heap = new MinHeap<>();
    }

    private AirwayNode getNeigbour(AirwayEdge edge, AirwayNode current) {
        if (edge.getFrom().equals(current)) {
            return edge.getTo();
        }
        return edge.getFrom();
    }

    public AirwayNode findStartNode(Graph graph) {
        AirwayNode best = null;

        for (AirwayNode node : graph.getNodes()) {
            if (node.getType() == NodeType.START) {
                return node;
            }

            if (node.getType() != NodeType.END) {
                if (best == null || node.getRow() < best.getRow()) {
                    best = node;
                }
            }
        }

        if (best != null) {
            return best;
        }

        for (AirwayNode node : graph.getNodes()) {
            if (best == null || node.getRow() < best.getRow()) {
                best = node;
            }
        }

        return best;
    }

    public AirwayNode findDeepestEndNodeDifferentFrom(AirwayNode start) {
        AirwayNode best = null;

        for (AirwayNode node : graph.getNodes()) {
            if (node.getType() == NodeType.END && !node.equals(start)) {
                if (best == null || node.getRow() > best.getRow()) {
                    best = node;
                }
            }
        }

        return best;
    }

    public AirwayNode findFirstEndNode() {
        for (AirwayNode node : graph.getNodes()) {
            if (node.getType() == NodeType.END) {
                return node;
            }
        }
        return null;
    }

    private double getEdgeCost(AirwayEdge edge) {
        double length = edge.getPathPixels().size();
        double widthPenalty = 10.0 / Math.max(edge.getAverageWidth(), 0.01);
        return length + widthPenalty;
    }

    public List<AirwayNode> dijkstra(AirwayNode start, AirwayNode target) {
        List<AirwayNode> path = new ArrayList<>();

        if (start == null || target == null) {
            return path;
        }

        Map<AirwayNode, Double> costs = new HashMap<>();
        Map<AirwayNode, AirwayNode> previous = new HashMap<>();

        for (AirwayNode node : graph.getNodes()) {
            costs.put(node, Double.MAX_VALUE);
            previous.put(node, null);
        }

        costs.put(start, 0.0);
        Set<AirwayNode> visited = new HashSet<>();

        heap = new MinHeap<>();
        heap.insert(start, 0.0);

        while (!heap.isEmpty()) {
            HeapNode<AirwayNode> entry = heap.extractMin();
            if (entry == null) {
                break;
            }

            AirwayNode current = entry.getValue();
            double currentCost = entry.getCost();

            if (currentCost > costs.get(current)) {
                continue;
            }

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            if (current.equals(target)) {
                break;
            }

            for (AirwayEdge edge : graph.getConnectedEdges(current)) {
                AirwayNode neighbour = getNeigbour(edge, current);

                if (visited.contains(neighbour)) {
                    continue;
                }

                double newCost = costs.get(current) + getEdgeCost(edge);

                if (newCost < costs.get(neighbour)) {
                    costs.put(neighbour, newCost);
                    previous.put(neighbour, current);
                    heap.insert(neighbour, newCost);
                }
            }
        }

        AirwayNode step = target;
        if (!start.equals(target) && previous.get(target) == null) {
            return path;
        }

        while (step != null) {
            path.add(0, step);
            step = previous.get(step);
        }

        return path;
    }
}