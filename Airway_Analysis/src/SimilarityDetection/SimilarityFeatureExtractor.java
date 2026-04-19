package SimilarityDetection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import GraphConstruction.Graph;
import model.AirwayImage;

public final class SimilarityFeatureExtractor {

    private SimilarityFeatureExtractor() {
    }

    public static PipelineResult runFullPipeline(AirwayImage finalCompressedImage) {
        return SimilarityPipeline.run(finalCompressedImage);
    }

    public static ComparisonResult compareWithDataset(
            AirwayImage finalCompressedImage,
            Path datasetFeaturesPath,
            int k
    ) throws IOException {
        PipelineResult pipelineResult = runFullPipeline(finalCompressedImage);
        return compareWithDataset(pipelineResult.getFeatures(), datasetFeaturesPath, k);
    }

    public static ComparisonResult compareWithDataset(
            SimilarityFeatures uploadedFeatures,
            Path datasetFeaturesPath,
            int k
    ) throws IOException {
        List<DatasetCase> datasetCases = DatasetFeatureFileReader.loadDatasetCases(datasetFeaturesPath);
        return KnnSimilarityComparator.compare(uploadedFeatures, datasetCases, k);
    }

    public static Path resolveDefaultDatasetFeaturePath() throws IOException {
        Path[] candidates = new Path[] {
                Paths.get("src", "dataset_features.txt"),
                Paths.get("dataset_features.txt"),
                Paths.get(System.getProperty("user.dir"), "src", "dataset_features.txt"),
                Paths.get(System.getProperty("user.dir"), "dataset_features.txt")
        };

        for (Path path : candidates) {
            if (path != null && Files.exists(path)) {
                return path;
            }
        }

        throw new IOException(
                "Could not find dataset_features.txt. " +
                "Make sure it is inside your project root or inside src."
        );
    }

    public static SimilarityFeatures extractFeatures(Graph graph) {
        return GraphFeatureCalculator.compute(graph);
    }
}
