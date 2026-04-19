package SimilarityDetection;

import GraphConstruction.Graph;
import model.AirwayImage;

public class PipelineResult {
    private final AirwayImage greyScaleImage;
    private final AirwayImage edgeImage;
    private final int[][] binaryMask;
    private final Graph graph;
    private final SimilarityFeatures features;

    public PipelineResult(
            AirwayImage greyScaleImage,
            AirwayImage edgeImage,
            int[][] binaryMask,
            Graph graph,
            SimilarityFeatures features
    ) {
        this.greyScaleImage = greyScaleImage;
        this.edgeImage = edgeImage;
        this.binaryMask = binaryMask;
        this.graph = graph;
        this.features = features;
    }

    public AirwayImage getGreyScaleImage() {
        return greyScaleImage;
    }

    public AirwayImage getEdgeImage() {
        return edgeImage;
    }

    public int[][] getBinaryMask() {
        return binaryMask;
    }

    public Graph getGraph() {
        return graph;
    }

    public SimilarityFeatures getFeatures() {
        return features;
    }
}
