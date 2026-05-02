package PathFindingDectection;

import GraphConstruction.AirwayNode;
import GraphConstruction.Graph;
import GraphConstruction.NodeType;

public class AirwayNodeSelector {

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

    public AirwayNode findDeepestEndNodeDifferentFrom(Graph graph, AirwayNode start) {
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

    public AirwayNode findFirstEndNode(Graph graph) {
        for (AirwayNode node : graph.getNodes()) {
            if (node.getType() == NodeType.END) {
                return node;
            }
        }
        return null;
    }
}