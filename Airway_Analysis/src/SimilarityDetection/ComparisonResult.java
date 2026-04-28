package SimilarityDetection;

import java.util.List;
import java.util.Locale;

public class ComparisonResult {
    private final SimilarityFeatures uploadedFeatures;
    private final NeighborMatch bestMatch;
    private final List<NeighborMatch> nearestMatches;
    private final int kUsed;
    private final String overallDecision;

    public ComparisonResult(
            SimilarityFeatures uploadedFeatures,
            NeighborMatch bestMatch,
            List<NeighborMatch> nearestMatches,
            int kUsed,
            String overallDecision
    ) {
        this.uploadedFeatures = uploadedFeatures;
        this.bestMatch = bestMatch;
        this.nearestMatches = nearestMatches;
        this.kUsed = kUsed;
        this.overallDecision = overallDecision;
    }

    public SimilarityFeatures getUploadedFeatures() {
        return uploadedFeatures;
    }

    public NeighborMatch getBestMatch() {
        return bestMatch;
    }

    public List<NeighborMatch> getNearestMatches() {
        return nearestMatches;
    }

    public int getKUsed() {
        return kUsed;
    }

    public String getOverallDecision() {
        return overallDecision;
    }

    public String buildNeighborSummary() {
        if (nearestMatches == null || nearestMatches.isEmpty()) {
            return "No nearest cases found.";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < nearestMatches.size(); i++) {
            NeighborMatch match = nearestMatches.get(i);

            if (i > 0) {
                builder.append(" | ");
            }

            builder.append("Case ")
                   .append(match.getDatasetCase().getCaseId())
                   .append(" (")
                   .append(formatPercentage(match.getSimilarityPercentage()))
                   .append(")");
        }

        return builder.toString();
    }

    private String formatPercentage(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
