package SimilarityDetection;

public class NeighborMatch {
    private final DatasetCase datasetCase;
    private final double distance;
    private final double similarityPercentage;
    private final String featureExplanation;

    public NeighborMatch(
            DatasetCase datasetCase,
            double distance,
            double similarityPercentage,
            String featureExplanation
    ) {
        this.datasetCase = datasetCase;
        this.distance = distance;
        this.similarityPercentage = similarityPercentage;
        this.featureExplanation = featureExplanation;
    }

    public DatasetCase getDatasetCase() {
        return datasetCase;
    }

    public double getDistance() {
        return distance;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    public String getFeatureExplanation() {
        return featureExplanation;
    }
}
