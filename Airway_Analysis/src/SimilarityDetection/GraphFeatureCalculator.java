package SimilarityDetection;

import java.util.ArrayDeque;
import DataStructure.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import GraphConstruction.AirwayEdge;
import GraphConstruction.AirwayNode;
import GraphConstruction.Graph;
import GraphConstruction.NodeType;
import GraphConstruction.PixelPoint;

public final class GraphFeatureCalculator {

    private static final double CLUSTER_RADIUS = 10.0;
    private static final double SHORT_ENDING_RATIO_THRESHOLD = 0.50;

    private GraphFeatureCalculator() {
    }

    public static SimilarityFeatures compute(Graph graph) {
        if (graph == null || graph.getNodes().isEmpty()) {
            return new SimilarityFeatures(0, 0, 0.0, 0.0, 0.0);
        }

        AirwayNode root = findRootNode(graph);
        if (root == null) {
            return new SimilarityFeatures(0, 0, 0.0, 0.0, 0.0);
        }

        List<NodeCluster> branchClusters = clusterNodesByType(graph.getNodes(), NodeType.BRANCH, CLUSTER_RADIUS);
        List<NodeCluster> endClusters = clusterNodesByType(graph.getNodes(), NodeType.END, CLUSTER_RADIUS);

        int branchPointCount = branchClusters.size();
        int branchCount = branchPointCount + endClusters.size();

        DijkstraResult dijkstra = runDijkstra(root, graph);

        double taperingSum = 0.0;
        double tortuositySum = 0.0;
        int validPathCount = 0;
        List<Double> terminalSegmentLengths = new ArrayList<>();

        for (NodeCluster endCluster : endClusters) {
            AirwayNode target = chooseReachableRepresentative(endCluster, dijkstra.distanceMap);

            if (target == null || target.equals(root)) {
                continue;
            }

            PathResult pathResult = reconstructPath(
                    root,
                    target,
                    dijkstra.previousNodeMap,
                    dijkstra.previousEdgeMap
            );

            if (pathResult.edges.isEmpty()) {
                continue;
            }

            double pathLength = pathResult.totalPathLength;
            if (pathLength <= 0.0) {
                continue;
            }

            double startWidth = pathResult.edges.get(0).getAverageWidth();
            double endWidth = pathResult.edges.get(pathResult.edges.size() - 1).getAverageWidth();

            double tapering = (startWidth - endWidth) / pathLength;
            if (tapering < 0.0) {
                tapering = 0.0;
            }

            double straightDistance = euclideanDistance(
                    root.getRow(), root.getCol(),
                    target.getRow(), target.getCol()
            );

            double tortuosity = 1.0;
            if (straightDistance > 0.0) {
                tortuosity = pathLength / straightDistance;
            }

            taperingSum += tapering;
            tortuositySum += tortuosity;
            validPathCount++;

            double terminalLength = computeEdgePathLength(
                    pathResult.edges.get(pathResult.edges.size() - 1)
            );
            terminalSegmentLengths.add(terminalLength);
        }

        double averageTapering = validPathCount == 0 ? 0.0 : taperingSum / validPathCount;
        double averageTortuosity = validPathCount == 0 ? 0.0 : tortuositySum / validPathCount;

        double abruptEndingRatio = 0.0;
        if (!terminalSegmentLengths.isEmpty()) {
            double averageTerminalLength = average(terminalSegmentLengths);
            int suspiciousShortEndings = 0;

            for (double length : terminalSegmentLengths) {
                if (length < averageTerminalLength * SHORT_ENDING_RATIO_THRESHOLD) {
                    suspiciousShortEndings++;
                }
            }

            abruptEndingRatio = (double) suspiciousShortEndings / terminalSegmentLengths.size();
        }

        return new SimilarityFeatures(
                branchCount,
                branchPointCount,
                averageTapering,
                averageTortuosity,
                abruptEndingRatio
        );
    }

    private static AirwayNode findRootNode(Graph graph) {
        for (AirwayNode node : graph.getNodes()) {
            if (node != null && node.getType() == NodeType.START) {
                return node;
            }
        }

        return graph.getNodes().isEmpty() ? null : graph.getNodes().get(0);
    }

    private static List<NodeCluster> clusterNodesByType(
            List<AirwayNode> allNodes,
            NodeType nodeType,
            double radius
    ) {
        List<AirwayNode> filtered = new ArrayList<>();

        for (AirwayNode node : allNodes) {
            if (node != null && node.getType() == nodeType) {
                filtered.add(node);
            }
        }

        List<NodeCluster> clusters = new ArrayList<>();
        boolean[] visited = new boolean[filtered.size()];

        for (int i = 0; i < filtered.size(); i++) {
            if (visited[i]) {
                continue;
            }

            NodeCluster cluster = new NodeCluster();
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(i);
            visited[i] = true;

            while (!queue.isEmpty()) {
                int currentIndex = queue.removeFirst();
                AirwayNode currentNode = filtered.get(currentIndex);
                cluster.add(currentNode);

                for (int j = 0; j < filtered.size(); j++) {
                    if (visited[j]) {
                        continue;
                    }

                    AirwayNode candidate = filtered.get(j);

                    if (euclideanDistance(
                            currentNode.getRow(), currentNode.getCol(),
                            candidate.getRow(), candidate.getCol()
                    ) <= radius) {
                        visited[j] = true;
                        queue.add(j);
                    }
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    private static AirwayNode chooseReachableRepresentative(
            NodeCluster cluster,
            Map<AirwayNode, Double> distanceMap
    ) {
        AirwayNode bestNode = null;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (AirwayNode node : cluster.getNodes()) {
            Double distance = distanceMap.get(node);

            if (distance == null || Double.isInfinite(distance)) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private static DijkstraResult runDijkstra(AirwayNode root, Graph graph) {
        Map<AirwayNode, Double> distanceMap = new HashMap<>();
        Map<AirwayNode, AirwayNode> previousNodeMap = new HashMap<>();
        Map<AirwayNode, AirwayEdge> previousEdgeMap = new HashMap<>();

        for (AirwayNode node : graph.getNodes()) {
            distanceMap.put(node, Double.POSITIVE_INFINITY);
        }

        distanceMap.put(root, 0.0);

        List<QueueEntry> queue = new ArrayList<>();
        queue.add(new QueueEntry(root, 0.0));

        while (!queue.isEmpty()) {
            queue.sort(Comparator.comparingDouble(QueueEntry::getDistance));
            QueueEntry entry = queue.remove(0);

            AirwayNode currentNode = entry.getNode();
            double currentDistance = entry.getDistance();

            if (currentDistance > distanceMap.get(currentNode)) {
                continue;
            }

            for (AirwayEdge edge : currentNode.getEdges()) {
                AirwayNode neighbor = getOtherNode(edge, currentNode);
                if (neighbor == null) {
                    continue;
                }

                double weight = computeEdgePathLength(edge);
                double newDistance = currentDistance + weight;

                if (newDistance < distanceMap.get(neighbor)) {
                    distanceMap.put(neighbor, newDistance);
                    previousNodeMap.put(neighbor, currentNode);
                    previousEdgeMap.put(neighbor, edge);
                    queue.add(new QueueEntry(neighbor, newDistance));
                }
            }
        }

        return new DijkstraResult(distanceMap, previousNodeMap, previousEdgeMap);
    }

    private static PathResult reconstructPath(
            AirwayNode root,
            AirwayNode target,
            Map<AirwayNode, AirwayNode> previousNodeMap,
            Map<AirwayNode, AirwayEdge> previousEdgeMap
    ) {
        List<AirwayEdge> reversedEdges = new ArrayList<>();
        AirwayNode current = target;

        while (current != null && !current.equals(root)) {
            AirwayEdge edge = previousEdgeMap.get(current);
            AirwayNode previous = previousNodeMap.get(current);

            if (edge == null || previous == null) {
                return new PathResult(Collections.emptyList(), 0.0);
            }

            reversedEdges.add(edge);
            current = previous;
        }

        Collections.reverse(reversedEdges);

        double totalPathLength = 0.0;
        for (AirwayEdge edge : reversedEdges) {
            totalPathLength += computeEdgePathLength(edge);
        }

        return new PathResult(reversedEdges, totalPathLength);
    }

    private static AirwayNode getOtherNode(AirwayEdge edge, AirwayNode currentNode) {
        if (edge == null || currentNode == null) {
            return null;
        }

        if (currentNode.equals(edge.getFrom())) {
            return edge.getTo();
        }

        if (currentNode.equals(edge.getTo())) {
            return edge.getFrom();
        }

        return null;
    }

    private static double computeEdgePathLength(AirwayEdge edge) {
        if (edge == null) {
            return 0.0;
        }

        List<PixelPoint> points = edge.getPathPixels();

        if (points != null && points.size() >= 2) {
            double length = 0.0;

            for (int i = 1; i < points.size(); i++) {
                PixelPoint previous = points.get(i - 1);
                PixelPoint current = points.get(i);

                length += euclideanDistance(
                        previous.getRow(), previous.getCol(),
                        current.getRow(), current.getCol()
                );
            }

            return length;
        }

        if (edge.getFrom() != null && edge.getTo() != null) {
            return euclideanDistance(
                    edge.getFrom().getRow(), edge.getFrom().getCol(),
                    edge.getTo().getRow(), edge.getTo().getCol()
            );
        }

        return 0.0;
    }

    private static double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        return sum / values.size();
    }

    private static double euclideanDistance(double row1, double col1, double row2, double col2) {
        double dr = row1 - row2;
        double dc = col1 - col2;
        return Math.sqrt((dr * dr) + (dc * dc));
    }

    private static class NodeCluster {
        private final List<AirwayNode> nodes = new ArrayList<>();

        public void add(AirwayNode node) {
            nodes.add(node);
        }

        public List<AirwayNode> getNodes() {
            return nodes;
        }
    }

    private static class DijkstraResult {
        private final Map<AirwayNode, Double> distanceMap;
        private final Map<AirwayNode, AirwayNode> previousNodeMap;
        private final Map<AirwayNode, AirwayEdge> previousEdgeMap;

        public DijkstraResult(
                Map<AirwayNode, Double> distanceMap,
                Map<AirwayNode, AirwayNode> previousNodeMap,
                Map<AirwayNode, AirwayEdge> previousEdgeMap
        ) {
            this.distanceMap = distanceMap;
            this.previousNodeMap = previousNodeMap;
            this.previousEdgeMap = previousEdgeMap;
        }
    }

    private static class QueueEntry {
        private final AirwayNode node;
        private final double distance;

        public QueueEntry(AirwayNode node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        public AirwayNode getNode() {
            return node;
        }

        public double getDistance() {
            return distance;
        }
    }

    private static class PathResult {
        private final List<AirwayEdge> edges;
        private final double totalPathLength;

        public PathResult(List<AirwayEdge> edges, double totalPathLength) {
            this.edges = edges;
            this.totalPathLength = totalPathLength;
        }
    }
}
