package SimilarityDetection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import DataStructure.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DatasetFeatureFileReader {

    private DatasetFeatureFileReader() {
    }

    public static List<DatasetCase> loadDatasetCases(Path datasetFeaturesPath) throws IOException {
        if (datasetFeaturesPath == null || !Files.exists(datasetFeaturesPath)) {
            throw new IOException("dataset_features.txt not found: " + datasetFeaturesPath);
        }

        List<String> lines = Files.readAllLines(datasetFeaturesPath);
        List<DatasetCase> cases = new ArrayList<>();

        for (String rawLine : lines) {
            if (rawLine == null) {
                continue;
            }

            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("case_id")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length < 6) {
                continue;
            }

            String caseId = parts[0].trim();
            int branchCount = Integer.parseInt(parts[1].trim());
            int branchPointCount = Integer.parseInt(parts[2].trim());
            double averageTapering = Double.parseDouble(parts[3].trim());
            double averageTortuosity = Double.parseDouble(parts[4].trim());
            double abruptEndingRatio = Double.parseDouble(parts[5].trim());

            SimilarityFeatures features = new SimilarityFeatures(
                    branchCount,
                    branchPointCount,
                    averageTapering,
                    averageTortuosity,
                    abruptEndingRatio
            );

            cases.add(new DatasetCase(caseId, features));
        }

        return cases;
    }
}
