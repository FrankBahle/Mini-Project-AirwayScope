package SimilarityDetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class KnnSimilarityComparator {

    private static final double MAX_NORMALIZED_DISTANCE = Math.sqrt(5.0);

    private KnnSimilarityComparator() {
    }

    public static ComparisonResult compare(
            SimilarityFeatures uploadedFeatures,
            List<DatasetCase> datasetCases,
            int k
    ) {
        if (uploadedFeatures == null) {
            throw new IllegalArgumentException("Uploaded features cannot be null.");
        }

        if (datasetCases == null || datasetCases.isEmpty()) {
            throw new IllegalArgumentException("Dataset cases cannot be empty.");
        }

        int actualK = Math.max(1, Math.min(k, datasetCases.size()));

        double[] mins = new double[5];
        double[] maxs = new double[5];
        initialiseMinMax(mins, maxs, uploadedFeatures, datasetCases);

        List<NeighborMatch> matches = new ArrayList<>();

        for (DatasetCase datasetCase : datasetCases) {
            double distance = calculateNormalizedDistance(
                    uploadedFeatures,
                    datasetCase.getFeatures(),
                    mins,
                    maxs
            );

            double similarityPercentage = Math.max(
                    0.0,
                    (1.0 - (distance / MAX_NORMALIZED_DISTANCE)) * 100.0
            );

            String featureExplanation = buildFeatureExplanation(
                    uploadedFeatures,
                    datasetCase.getFeatures(),
                    datasetCase.getCaseId()
            );

            matches.add(new NeighborMatch(
                    datasetCase,
                    distance,
                    similarityPercentage,
                    featureExplanation
            ));
        }

        matches.sort(Comparator.comparingDouble(NeighborMatch::getDistance));

        List<NeighborMatch> nearest = new ArrayList<>();
        for (int i = 0; i < actualK; i++) {
            nearest.add(matches.get(i));
        }

        NeighborMatch bestMatch = nearest.get(0);

        String overallDecision =
                "Using KNN with the " + actualK + " nearest reference cases, " +
                "the uploaded case is most similar to Case " + bestMatch.getDatasetCase().getCaseId() +
                " with a similarity score of " + formatPercentage(bestMatch.getSimilarityPercentage()) + ".";

        return new ComparisonResult(
                uploadedFeatures,
                bestMatch,
                Collections.unmodifiableList(nearest),
                actualK,
                overallDecision
        );
    }

    private static void initialiseMinMax(
            double[] mins,
            double[] maxs,
            SimilarityFeatures uploadedFeatures,
            List<DatasetCase> datasetCases
    ) {
        double[] uploadedVector = uploadedFeatures.toVector();

        for (int i = 0; i < mins.length; i++) {
            mins[i] = uploadedVector[i];
            maxs[i] = uploadedVector[i];
        }

        for (DatasetCase datasetCase : datasetCases) {
            double[] vector = datasetCase.getFeatures().toVector();

            for (int i = 0; i < vector.length; i++) {
                if (vector[i] < mins[i]) {
                    mins[i] = vector[i];
                }
                if (vector[i] > maxs[i]) {
                    maxs[i] = vector[i];
                }
            }
        }
    }

    private static double calculateNormalizedDistance(
            SimilarityFeatures uploadedFeatures,
            SimilarityFeatures datasetFeatures,
            double[] mins,
            double[] maxs
    ) {
        double[] uploaded = uploadedFeatures.toVector();
        double[] dataset = datasetFeatures.toVector();

        double sum = 0.0;

        for (int i = 0; i < uploaded.length; i++) {
            double range = maxs[i] - mins[i];
            double uploadedNormalised = range == 0.0 ? 0.0 : (uploaded[i] - mins[i]) / range;
            double datasetNormalised = range == 0.0 ? 0.0 : (dataset[i] - mins[i]) / range;

            double difference = uploadedNormalised - datasetNormalised;
            sum += difference * difference;
        }

        return Math.sqrt(sum);
    }

    private static String buildFeatureExplanation(
            SimilarityFeatures uploadedFeatures,
            SimilarityFeatures matchedFeatures,
            String caseId
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("Case ")
               .append(caseId)
               .append(" is the closest structural match because ");

        builder.append("branch count is ")
               .append(describeCloseness(
                       Math.abs(uploadedFeatures.getBranchCount() - matchedFeatures.getBranchCount()),
                       3.0,
                       8.0
               ))
               .append(" (")
               .append(uploadedFeatures.getBranchCount())
               .append(" vs ")
               .append(matchedFeatures.getBranchCount())
               .append("), ");

        builder.append("branch-point count is ")
               .append(describeCloseness(
                       Math.abs(uploadedFeatures.getBranchPointCount() - matchedFeatures.getBranchPointCount()),
                       2.0,
                       6.0
               ))
               .append(" (")
               .append(uploadedFeatures.getBranchPointCount())
               .append(" vs ")
               .append(matchedFeatures.getBranchPointCount())
               .append("), ");

        builder.append("average tapering is ")
               .append(describeCloseness(
                       Math.abs(uploadedFeatures.getAverageTapering() - matchedFeatures.getAverageTapering()),
                       0.002,
                       0.01
               ))
               .append(" (")
               .append(formatDecimal(uploadedFeatures.getAverageTapering()))
               .append(" vs ")
               .append(formatDecimal(matchedFeatures.getAverageTapering()))
               .append("), ");

        builder.append("average tortuosity is ")
               .append(describeCloseness(
                       Math.abs(uploadedFeatures.getAverageTortuosity() - matchedFeatures.getAverageTortuosity()),
                       0.10,
                       0.30
               ))
               .append(" (")
               .append(formatDecimal(uploadedFeatures.getAverageTortuosity()))
               .append(" vs ")
               .append(formatDecimal(matchedFeatures.getAverageTortuosity()))
               .append("), ");

        builder.append("and abrupt-ending ratio is ")
               .append(describeCloseness(
                       Math.abs(uploadedFeatures.getAbruptEndingRatio() - matchedFeatures.getAbruptEndingRatio()),
                       0.08,
                       0.20
               ))
               .append(" (")
               .append(formatDecimal(uploadedFeatures.getAbruptEndingRatio()))
               .append(" vs ")
               .append(formatDecimal(matchedFeatures.getAbruptEndingRatio()))
               .append(").");

        return builder.toString();
    }

    private static String describeCloseness(double difference, double veryCloseThreshold, double closeThreshold) {
        if (difference <= veryCloseThreshold) {
            return "very close";
        }
        if (difference <= closeThreshold) {
            return "close";
        }
        return "less close";
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private static String formatPercentage(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
