package GraphConstruction;

import DataStructure.*;
import java.util.List;
import java.util.Objects;

public class AirwayEdge {

    private final AirwayNode from;
    private final AirwayNode to;

    // skeleton path between the 2 nodes
    private final List<PixelPoint> pathPixels;

    // width measured at each skeleton pixel using the ORIGINAL binary mask
    private final List<Double> localWidths;

    private final double averageWidth;
    private final double minWidth;
    private final double maxWidth;

    public AirwayEdge(AirwayNode from, AirwayNode to,
                      List<PixelPoint> pathPixels,
                      List<Double> localWidths) {

        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.pathPixels = new ArrayList<>(Objects.requireNonNull(pathPixels));
        this.localWidths = new ArrayList<>(Objects.requireNonNull(localWidths));

        if (this.pathPixels.size() != this.localWidths.size()) {
            throw new IllegalArgumentException(
                "pathPixels and localWidths must have the same size"
            );
        }

        if (this.localWidths.isEmpty()) {
            this.averageWidth = 0.0;
            this.minWidth = 0.0;
            this.maxWidth = 0.0;
        } else {
            double sum = 0.0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (double w : this.localWidths) {
                sum += w;
                if (w < min) min = w;
                if (w > max) max = w;
            }

            this.averageWidth = sum / this.localWidths.size();
            this.minWidth = min;
            this.maxWidth = max;
        }
    }

    public AirwayNode getFrom() {
        return from;
    }

    public AirwayNode getTo() {
        return to;
    }

    public List<PixelPoint> getPathPixels() {
        return pathPixels;
    }

    public List<Double> getLocalWidths() {
        return localWidths;
    }

    public double getAverageWidth() {
        return averageWidth;
    }

    public double getMinWidth() {
        return minWidth;
    }

    public double getMaxWidth() {
        return maxWidth;
    }

    @Override
    public String toString() {
        return "AirwayEdge{" +
                "from=" + from +
                ", to=" + to +
                ", pathPixels=" + pathPixels.size() +
                ", avgWidth=" + averageWidth +
                ", minWidth=" + minWidth +
                ", maxWidth=" + maxWidth +
                '}';
    }

    // undirected edge equality
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AirwayEdge)) return false;

        AirwayEdge other = (AirwayEdge) obj;

        return (from.equals(other.from) && to.equals(other.to)) ||
               (from.equals(other.to) && to.equals(other.from));
    }

    @Override
    public int hashCode() {
        int h1 = from.hashCode();
        int h2 = to.hashCode();
        return h1 <= h2 ? Objects.hash(h1, h2) : Objects.hash(h2, h1);
    }
}