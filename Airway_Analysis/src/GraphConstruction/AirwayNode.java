package GraphConstruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AirwayNode {
    private final int row;
    private final int col;
    private NodeType type;
    private final List<AirwayEdge> edges;
    private final PixelPoint pixelPoint;

    public AirwayNode(PixelPoint pixelPoint, NodeType type) {
        this.pixelPoint = Objects.requireNonNull(pixelPoint, "pixelPoint cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.row = pixelPoint.getRow();
        this.col = pixelPoint.getCol();
        this.edges = new ArrayList<>();
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    public List<AirwayEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public void addEdge(AirwayEdge edge) {
        if (edge != null && !edges.contains(edge)) {
            edges.add(edge);
        }
    }

    public PixelPoint getPixelPoint() {
        return pixelPoint;
    }

    @Override
    public String toString() {
        return "AirwayNode{" +
               "row=" + row +
               ", col=" + col +
               ", type=" + type +
               '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AirwayNode)) return false;
        AirwayNode other = (AirwayNode) obj;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}