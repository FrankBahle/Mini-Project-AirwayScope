package SimilarityDetection;

public class DatasetCase {
    private final String caseId;
    private final SimilarityFeatures features;

    public DatasetCase(String caseId, SimilarityFeatures features) {
        this.caseId = caseId;
        this.features = features;
    }

    public String getCaseId() {
        return caseId;
    }

    public SimilarityFeatures getFeatures() {
        return features;
    }
}
