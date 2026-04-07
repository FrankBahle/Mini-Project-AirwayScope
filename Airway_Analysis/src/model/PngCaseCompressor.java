package model;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class PngCaseCompressor {

    public static class CompressionResult {
        private int numberOfSlicesUsed;
        private AirwayImage finalImage;

        public CompressionResult() {
            numberOfSlicesUsed = 0;
            finalImage = null;
        }

        public CompressionResult(int numberOfSlicesUsed, AirwayImage finalImage) {
            this.numberOfSlicesUsed = numberOfSlicesUsed;
            this.finalImage = finalImage;
        }

        public int getNumberOfSlicesUsed() {
            return numberOfSlicesUsed;
        }

        public void setNumberOfSlicesUsed(int numberOfSlicesUsed) {
            this.numberOfSlicesUsed = numberOfSlicesUsed;
        }

        public AirwayImage getFinalImage() {
            return finalImage;
        }

        public void setFinalImage(AirwayImage finalImage) {
            this.finalImage = finalImage;
        }
    }

    public static CompressionResult compressAirwayMasks(Path caseOutputDir) throws Exception {
        Path airwayDir = caseOutputDir.resolve("airway_coronal_png");

        if (!Files.exists(airwayDir) || !Files.isDirectory(airwayDir)) {
            throw new Exception("Airway PNG folder not found: " + airwayDir);
        }

        Path firstFile = getFirstPng(airwayDir);
        if (firstFile == null) {
            throw new Exception("No PNG files found in: " + airwayDir);
        }

        Image firstImage = new Image(firstFile.toUri().toString());
        int width = (int) firstImage.getWidth();
        int height = (int) firstImage.getHeight();

        int[][] counts = new int[height][width];
        int numberOfSlicesUsed = 0;

        try (Stream<Path> stream = Files.list(airwayDir)) {
            Path[] files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toArray(Path[]::new);

            for (Path file : files) {
                Image img = new Image(file.toUri().toString());
                PixelReader reader = img.getPixelReader();

                if (reader == null) {
                    continue;
                }

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Color color = reader.getColor(x, y);
                        double brightness = color.getBrightness();

                        if (brightness > 0.0) {
                            counts[y][x]++;
                        }
                    }
                }

                numberOfSlicesUsed++;
            }
        }

        int maxCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (counts[y][x] > maxCount) {
                    maxCount = counts[y][x];
                }
            }
        }

        if (maxCount == 0) {
            maxCount = 1;
        }

        AirwayImage finalImage = new AirwayImage(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) Math.round((counts[y][x] * 255.0) / maxCount);
                finalImage.setPixel(x, y, value);
            }
        }

        return new CompressionResult(numberOfSlicesUsed, finalImage);
    }

    private static Path getFirstPng(Path folder) throws Exception {
        try (Stream<Path> stream = Files.list(folder)) {
            Optional<Path> first = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .findFirst();

            if (first.isPresent()) {
                return first.get();
            }
        }

        return null;
    }
}