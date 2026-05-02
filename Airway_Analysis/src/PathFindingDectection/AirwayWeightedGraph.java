package PathFindingDectection;

import java.util.ArrayList;
import java.util.List;

import GraphConstruction.AirwayEdge;
import GraphConstruction.AirwayNode;
import GraphConstruction.Graph;

public class AirwayWeightedGraph implements WeightedGraph<AirwayNode> {
    private final Graph graph;

    public AirwayWeightedGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public List<AirwayNode> getNodes() {
        return graph.getNodes();
    }

    @Override
    public List<AirwayNode> getNeighbours(AirwayNode node) {
        List<AirwayNode> neighbours = new ArrayList<>();

        for (AirwayEdge edge : graph.getConnectedEdges(node)) {
            if (edge.getFrom().equals(node)) {
                neighbours.add(edge.getTo());
            } else {
                neighbours.add(edge.getFrom());
            }
        }

        return neighbours;
    }

    @Override
    public double getCost(AirwayNode from, AirwayNode to) {
        for (AirwayEdge edge : graph.getConnectedEdges(from)) {
            AirwayNode neighbour;
            if (edge.getFrom().equals(from)) {
                neighbour = edge.getTo();
            } else {
                neighbour = edge.getFrom();
            }

            if (neighbour.equals(to)) {
                double length = edge.getPathPixels().size();
                double widthPenalty = 10.0 / Math.max(edge.getAverageWidth(), 0.01);
                return length + widthPenalty;
            }
        }

        return Double.MAX_VALUE;
    }
}