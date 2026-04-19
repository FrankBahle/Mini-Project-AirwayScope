package SimilarityDetection;

import GraphConstruction.Graph;
import GraphConstruction.GraphBuild;
import filter.EdgeDetection;
import filter.ImageFilter;
import model.AirwayImage;

public final class SimilarityPipeline {

    private SimilarityPipeline() {
    }

    public static PipelineResult run(AirwayImage finalCompressedImage) {
        if (finalCompressedImage == null) {
            throw new IllegalArgumentException("Final compressed image cannot be null.");
        }

        AirwayImage imageCopy = copyImage(finalCompressedImage);

        ImageFilter greyScaleFilter = new ImageFilter(imageCopy);
        greyScaleFilter.applyGrayScale();
        AirwayImage greyScaleImage = greyScaleFilter.getGreyScaleImage();

        EdgeDetection edgeDetection = new EdgeDetection(greyScaleImage, 1);
        AirwayImage edgeImage = edgeDetection.getEdgeImage();

        ImageFilter binaryMaskFilter = new ImageFilter(edgeImage);
        binaryMaskFilter.setThreshold(100);
        binaryMaskFilter.applyMask();
        int[][] binaryMask = binaryMaskFilter.getBinaryMask();

        GraphBuild builder = new GraphBuild(binaryMask);
        Graph graph = builder.buildGraph();

        SimilarityFeatures features = GraphFeatureCalculator.compute(graph);

        return new PipelineResult(greyScaleImage, edgeImage, binaryMask, graph, features);
    }

    private static AirwayImage copyImage(AirwayImage original) {
        AirwayImage copy = new AirwayImage(original.getWidth(), original.getHeight());

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                copy.setPixel(x, y, original.getPixel(x, y).getValue());
            }
        }

        return copy;
    }
}
