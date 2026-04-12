package GraphConstruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphOverlay {

    public static class OverlayLine {
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        public OverlayLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public double getX1() { return x1; }
        public double getY1() { return y1; }
        public double getX2() { return x2; }
        public double getY2() { return y2; }
    }

    public static class OverlayNode {
        private final double x;
        private final double y;
        private final NodeType type;

        public OverlayNode(double x, double y, NodeType type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public NodeType getType() { return type; }
    }

    public static class OverlayData {
        private final List<OverlayLine> lines;
        private final List<OverlayNode> nodes;

        public OverlayData(List<OverlayLine> lines, List<OverlayNode> nodes) {
            this.lines = new ArrayList<>(lines);
            this.nodes = new ArrayList<>(nodes);
        }

        public List<OverlayLine> getLines() {
            return Collections.unmodifiableList(lines);
        }

        public List<OverlayNode> getNodes() {
            return Collections.unmodifiableList(nodes);
        }
    }

    private GraphOverlay() {
    }

    public static OverlayData buildOverlayData(
            Graph graph,
            double imageWidth,
            double imageHeight,
            int maskRows,
            int maskCols
    ) {
        if (graph == null) {
            throw new IllegalArgumentException("graph cannot be null");
        }

        double scaleX = imageWidth / maskCols;
        double scaleY = imageHeight / maskRows;

        List<OverlayLine> lines = new ArrayList<>();
        List<OverlayNode> nodes = new ArrayList<>();

        for (AirwayEdge edge : graph.getEdges()) {
            if (edge == null || edge.getFrom() == null || edge.getTo() == null) {
                continue;
            }

            double x1 = edge.getFrom().getCol() * scaleX;
            double y1 = edge.getFrom().getRow() * scaleY;
            double x2 = edge.getTo().getCol() * scaleX;
            double y2 = edge.getTo().getRow() * scaleY;

            lines.add(new OverlayLine(x1, y1, x2, y2));
        }

        for (AirwayNode node : graph.getNodes()) {
            if (node == null) {
                continue;
            }

            double x = node.getCol() * scaleX;
            double y = node.getRow() * scaleY;

            nodes.add(new OverlayNode(x, y, node.getType()));
        }

        return new OverlayData(lines, nodes);
    }
}