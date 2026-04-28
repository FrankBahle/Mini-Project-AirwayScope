package model;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import SimilarityDetection.PipelineResult;
import SimilarityDetection.SimilarityFeatureExtractor;
import SimilarityDetection.SimilarityFeatures;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class BatchDatasetGenerator extends Application {

    private Label statusLabel;
    private ProgressBar progressBar;
    private Button startButton;
    private Button selectFolderButton;
    private Label caseLabel;
    private Path selectedBaseDir;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Batch Dataset Generator");

        statusLabel = new Label("Step 1: Select AeroPath folder");
        caseLabel = new Label("No folder selected");
        progressBar = new ProgressBar(0);
        selectFolderButton = new Button("Select AeroPath Folder");
        startButton = new Button("Start Batch Generation");
        startButton.setDisable(true);

        selectFolderButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select AeroPath folder (contains folders 1,2,3...27)");
            File folder = chooser.showDialog(primaryStage);
            if (folder != null) {
                selectedBaseDir = folder.toPath();
                caseLabel.setText("Selected: " + selectedBaseDir);
                statusLabel.setText("Step 2: Click Start Generation");
                startButton.setDisable(false);
            }
        });

        startButton.setOnAction(e -> startGeneration());

        VBox root = new VBox(15, selectFolderButton, caseLabel, statusLabel, progressBar, startButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setMinWidth(500);
        root.setMinHeight(250);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startGeneration() {
        if (selectedBaseDir == null) {
            statusLabel.setText("Please select folder first");
            return;
        }

        startButton.setDisable(true);
        selectFolderButton.setDisable(true);
        statusLabel.setText("Status: Generating... This will take hours.");

        Thread thread = new Thread(() -> {
            try {
                generateAllCases();
                Platform.runLater(() -> {
                    statusLabel.setText("Status: COMPLETE!");
                    startButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Status: ERROR - " + ex.getMessage());
                    startButton.setDisable(false);
                });
                ex.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void generateAllCases() throws Exception {
        Path outputRoot = Paths.get("output");
        Path finalImagesDir = outputRoot.resolve("batch_final_images");
        Files.createDirectories(finalImagesDir);

        Path featuresPath = Paths.get("dataset_features_new.txt");
        PrintWriter writer = new PrintWriter(new FileWriter(featuresPath.toFile()));
        writer.println("case_id | branchCount | branchPointCount | averageTapering | averageTortuosity | abruptEndingRatio");

        int successCount = 0;

        for (int caseNum = 1; caseNum <= 27; caseNum++) {
            final int currentCase = caseNum;
            Platform.runLater(() -> {
                caseLabel.setText("Case: " + currentCase + " / 27");
                progressBar.setProgress((double) currentCase / 27);
                statusLabel.setText("Processing case " + currentCase + "...");
            });

            String caseId = String.valueOf(caseNum);
            Path caseDir = selectedBaseDir.resolve(caseId);

            if (!Files.exists(caseDir)) {
                System.err.println("Case " + caseId + " folder not found: " + caseDir);
                continue;
            }

            System.out.println("=========================================");
            System.out.println("Processing case " + caseId);
            System.out.println("=========================================");

            // Convert CT to PNG slices
            ConvertCT.ConversionResult conversionResult = ConvertCT.convertCaseFromFolder(
                    caseDir,
                    outputRoot,
                    true,      // onlyNonemptyAirway
                    -1000.0,   // clipMin
                    400.0      // clipMax
            );

            System.out.println("Converted " + conversionResult.savedCount + " slices for case " + caseId);

            // Compress PNG slices to final image
            Path caseOutputDir = outputRoot.resolve(caseId);
            PngCaseCompressor.CompressionResult compressionResult = PngCaseCompressor.compressAirwayMasks(caseOutputDir);
            AirwayImage finalImage = compressionResult.getFinalImage();

            // Save final image
            String finalImageName = "case_" + caseId + ".png";
            Path finalImagePath = finalImagesDir.resolve(finalImageName);
            saveAirwayImageAsPng(finalImage, finalImagePath);

            // Extract features
            PipelineResult pipelineResult = SimilarityFeatureExtractor.runFullPipeline(finalImage);
            SimilarityFeatures features = pipelineResult.getFeatures();

            // Write to features file
            writer.printf(Locale.US, "%s | %d | %d | %.6f | %.6f | %.6f%n",
                    caseId,
                    features.getBranchCount(),
                    features.getBranchPointCount(),
                    features.getAverageTapering(),
                    features.getAverageTortuosity(),
                    features.getAbruptEndingRatio()
            );
            writer.flush();

            successCount++;
            System.out.println("Case " + caseId + " DONE | branchCount=" + features.getBranchCount());
        }

        writer.close();

        System.out.println("=========================================");
        System.out.println("COMPLETE! Processed " + successCount + "/27 cases");
        System.out.println("Features: dataset_features_new.txt");
        System.out.println("=========================================");
    }

    private void saveAirwayImageAsPng(AirwayImage image, Path path) throws Exception {
        javafx.scene.image.WritableImage fxImage = image.displayPixels();
        java.awt.image.BufferedImage buffered = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);
        javax.imageio.ImageIO.write(buffered, "png", path.toFile());
    }

    public static void main(String[] args) {
        launch(args);
    }
}