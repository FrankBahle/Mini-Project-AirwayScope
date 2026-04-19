package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import GraphConstruction.AirwayNode;
import GraphConstruction.Graph;
import GraphConstruction.GraphBuild;
import GraphConstruction.GraphOverlay;
import PathFindingDectection.PathFind;
import PathFindingDectection.SuspiciousNodeDectector;
import model.AirwayImage;
import SimilarityDetection.ComparisonResult;
import model.ConvertCT;
import SimilarityDetection.PipelineResult;
import model.PngCaseCompressor;
import SimilarityDetection.SimilarityFeatureExtractor;
import SimilarityDetection.SimilarityFeatures;
import filter.EdgeDetection;
import filter.ImageFilter;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class MainUI {

    private final ImageView originalImageView = new ImageView();
    private final ImageView finalImageView = new ImageView();
    private final ImageView graphImageView = new ImageView();
    private final ImageView edgeImageView = new ImageView();

    private final Label originalPlaceholder = new Label("No case folder selected");
    private final Label finalPlaceholder = new Label("No final compressed image yet");
    private final Label edgePlaceholder = new Label("No edge image yet");
    private final Label graphPlaceholder = new Label("No graph image yet");

    private final Label affectedBranchValue = new Label("—");
    private final Label narrowingValue = new Label("—");
    private final Label pathTraceValue = new Label("—");
    private final Label conditionValue = new Label("—");
    private final Label statusLabel = new Label("Status: Waiting for case folder upload...");

    private final Label topSuspiciousNodeValue = new Label("—");
    private final Label topReasonValue = new Label("—");

    private final Label branchCountValue = new Label("—");
    private final Label branchPointCountValue = new Label("—");
    private final Label averageTaperingValue = new Label("—");
    private final Label averageTortuosityValue = new Label("—");
    private final Label abruptEndingRatioValue = new Label("—");
    private final Label extractedFeatureExplanationLabel =
            new Label("No structural features extracted yet.");

    private final Label knnValue = new Label("—");
    private final Label bestMatchCaseValue = new Label("—");
    private final Label bestMatchPercentageValue = new Label("—");
    private final Label nearestCasesValue = new Label("—");
    private final Label similarityExplanationLabel =
            new Label("No similarity comparison has been performed yet.");

    private final ListView<SuspiciousItem> suspiciousListView = new ListView<>();

    private File selectedCaseFolder;
    private Path latestOutputDir;
    private AirwayImage latestCompressedAirwayImage;

    private Button extractFeaturesBtn;

    private PathFind currentPathFinder;
    private AirwayNode currentStartNode;
    private Image currentBaseImage;
    private GraphOverlay.OverlayData currentOverlayData;
    private int currentMaskRows;
    private int currentMaskCols;

    private Map<AirwayNode, Double> currentCombinedScores = new HashMap<>();
    private List<AirwayNode> currentSuspiciousNodes = new ArrayList<>();

    private static class SuspiciousItem {
        private final AirwayNode node;
        private final double score;
        private final String reason;

        public SuspiciousItem(AirwayNode node, double score, String reason) {
            this.node = node;
            this.score = score;
            this.reason = reason;
        }

        public AirwayNode getNode() {
            return node;
        }

        public double getScore() {
            return score;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return "row=" + node.getRow()
                    + ", col=" + node.getCol()
                    + " | score=" + String.format("%.3f", score)
                    + " | " + reason;
        }
    }

    private static class FeatureTaskResult {
        private final PipelineResult pipelineResult;
        private final ComparisonResult comparisonResult;
        private final String similarityError;

        public FeatureTaskResult(
                PipelineResult pipelineResult,
                ComparisonResult comparisonResult,
                String similarityError
        ) {
            this.pipelineResult = pipelineResult;
            this.comparisonResult = comparisonResult;
            this.similarityError = similarityError;
        }

        public PipelineResult getPipelineResult() {
            return pipelineResult;
        }

        public ComparisonResult getComparisonResult() {
            return comparisonResult;
        }

        public String getSimilarityError() {
            return similarityError;
        }
    }

    public Scene createScene(Stage stage) {

        Label titleLabel = new Label("Airway Analysis System");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#1f2937"));

        Label subtitleLabel = new Label(
                "Convert CT case folders to PNG slices, then compress them into one airway image"
        );
        subtitleLabel.setFont(Font.font("Segoe UI", 13));
        subtitleLabel.setTextFill(Color.web("#6b7280"));

        VBox titleBox = new VBox(4, titleLabel, subtitleLabel);

        Button uploadBtn = new Button("Upload Case Folder");
        Button processBtn = new Button("Process Case");
        extractFeaturesBtn = new Button("Extract Features");
        Button clearBtn = new Button("Clear");

        stylePrimaryButton(uploadBtn);
        styleSuccessButton(processBtn);
        stylePrimaryButton(extractFeaturesBtn);
        styleDangerButton(clearBtn);

        extractFeaturesBtn.setDisable(true);

        HBox buttonBox = new HBox(10, uploadBtn, processBtn, extractFeaturesBtn, clearBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(20, titleBox, spacer, buttonBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));
        header.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e5e7eb;" +
                "-fx-border-width: 0 0 1 0;"
        );

        VBox originalCard = createImageCard("CT Preview", originalImageView, originalPlaceholder);
        VBox finalCard = createImageCard("Final Compressed Image", finalImageView, finalPlaceholder);
        VBox edgeCard = createImageCard("Edge Image", edgeImageView, edgePlaceholder);
        VBox graphCard = createImageCard("Graph Image", graphImageView, graphPlaceholder);

        HBox imageSection = new HBox(15, originalCard, finalCard, edgeCard, graphCard);
        imageSection.setPadding(new Insets(20, 20, 10, 20));

        ScrollPane imageScrollPane = new ScrollPane(imageSection);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setPannable(true);
        imageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        imageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageScrollPane.setStyle("-fx-background: #f7f9fc; -fx-background-color: #f7f9fc;");

        Label resultsTitle = new Label("Analysis Results");
        resultsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        resultsTitle.setTextFill(Color.web("#1f2937"));

        GridPane resultsGrid = new GridPane();
        resultsGrid.setHgap(20);
        resultsGrid.setVgap(14);

        resultsGrid.add(createKeyLabel("Selected Case:"), 0, 0);
        resultsGrid.add(createValueLabel(affectedBranchValue), 1, 0);

        resultsGrid.add(createKeyLabel("Saved PNG Slices:"), 0, 1);
        resultsGrid.add(createValueLabel(narrowingValue), 1, 1);

        resultsGrid.add(createKeyLabel("Output Folder:"), 0, 2);
        resultsGrid.add(createValueLabel(pathTraceValue), 1, 2);

        resultsGrid.add(createKeyLabel("Next Step:"), 0, 3);
        resultsGrid.add(createValueLabel(conditionValue), 1, 3);

        resultsGrid.add(createKeyLabel("Top Suspicious Node:"), 0, 4);
        resultsGrid.add(createValueLabel(topSuspiciousNodeValue), 1, 4);

        resultsGrid.add(createKeyLabel("Top Reason:"), 0, 5);
        resultsGrid.add(createValueLabel(topReasonValue), 1, 5);

        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#4b5563"));
        statusLabel.setWrapText(true);

        VBox resultsCard = new VBox(
                15,
                resultsTitle,
                new Separator(),
                resultsGrid,
                new Separator(),
                statusLabel
        );
        resultsCard.setPadding(new Insets(20));
        resultsCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #dbe3ea;" +
                "-fx-border-radius: 12;"
        );

        Label featuresTitle = new Label("Extracted Features");
        featuresTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        featuresTitle.setTextFill(Color.web("#1f2937"));

        GridPane featuresGrid = new GridPane();
        featuresGrid.setHgap(20);
        featuresGrid.setVgap(14);

        featuresGrid.add(createKeyLabel("Feature 1: Branch Count"), 0, 0);
        featuresGrid.add(createValueLabel(branchCountValue), 1, 0);

        featuresGrid.add(createKeyLabel("Feature 2: Branch Point Count"), 0, 1);
        featuresGrid.add(createValueLabel(branchPointCountValue), 1, 1);

        featuresGrid.add(createKeyLabel("Feature 3: Average Tapering"), 0, 2);
        featuresGrid.add(createValueLabel(averageTaperingValue), 1, 2);

        featuresGrid.add(createKeyLabel("Feature 4: Average Tortuosity"), 0, 3);
        featuresGrid.add(createValueLabel(averageTortuosityValue), 1, 3);

        featuresGrid.add(createKeyLabel("Feature 5: Abrupt-Ending Ratio"), 0, 4);
        featuresGrid.add(createValueLabel(abruptEndingRatioValue), 1, 4);

        extractedFeatureExplanationLabel.setWrapText(true);
        extractedFeatureExplanationLabel.setFont(Font.font("Segoe UI", 13));
        extractedFeatureExplanationLabel.setTextFill(Color.web("#334155"));

        VBox featuresCard = new VBox(
                15,
                featuresTitle,
                new Separator(),
                featuresGrid,
                new Separator(),
                extractedFeatureExplanationLabel
        );
        featuresCard.setPadding(new Insets(20));
        featuresCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #dbe3ea;" +
                "-fx-border-radius: 12;"
        );

        Label similarityTitle = new Label("KNN Similarity Results");
        similarityTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        similarityTitle.setTextFill(Color.web("#1f2937"));

        GridPane similarityGrid = new GridPane();
        similarityGrid.setHgap(20);
        similarityGrid.setVgap(14);

        similarityGrid.add(createKeyLabel("K Used:"), 0, 0);
        similarityGrid.add(createValueLabel(knnValue), 1, 0);

        similarityGrid.add(createKeyLabel("Most Similar Case:"), 0, 1);
        similarityGrid.add(createValueLabel(bestMatchCaseValue), 1, 1);

        similarityGrid.add(createKeyLabel("Similarity Percentage:"), 0, 2);
        similarityGrid.add(createValueLabel(bestMatchPercentageValue), 1, 2);

        similarityGrid.add(createKeyLabel("Nearest Cases:"), 0, 3);
        similarityGrid.add(createValueLabel(nearestCasesValue), 1, 3);

        similarityExplanationLabel.setWrapText(true);
        similarityExplanationLabel.setFont(Font.font("Segoe UI", 13));
        similarityExplanationLabel.setTextFill(Color.web("#334155"));

        VBox similarityCard = new VBox(
                15,
                similarityTitle,
                new Separator(),
                similarityGrid,
                new Separator(),
                similarityExplanationLabel
        );
        similarityCard.setPadding(new Insets(20));
        similarityCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #dbe3ea;" +
                "-fx-border-radius: 12;"
        );

        VBox leftBottomCards = new VBox(15, resultsCard, featuresCard, similarityCard);
        leftBottomCards.setPadding(new Insets(0, 0, 10, 20));

        ScrollPane leftBottomScrollPane = new ScrollPane(leftBottomCards);
        leftBottomScrollPane.setFitToWidth(true);
        leftBottomScrollPane.setPannable(true);
        leftBottomScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftBottomScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftBottomScrollPane.setStyle("-fx-background: #f7f9fc; -fx-background-color: #f7f9fc;");
        leftBottomScrollPane.setPrefViewportHeight(250);
        leftBottomScrollPane.setMinHeight(250);
        leftBottomScrollPane.setMaxHeight(250);

        VBox suspiciousCard = createSuspiciousListCard();
        suspiciousCard.setMinWidth(300);
        suspiciousCard.setPrefWidth(300);
        suspiciousCard.setMaxWidth(300);
        suspiciousCard.setMinHeight(250);
        suspiciousCard.setPrefHeight(250);
        suspiciousCard.setMaxHeight(250);

        HBox bottomSection = new HBox(15, leftBottomScrollPane, suspiciousCard);
        HBox.setHgrow(leftBottomScrollPane, Priority.ALWAYS);
        bottomSection.setPadding(new Insets(0, 20, 20, 20));
        bottomSection.setMinHeight(270);
        bottomSection.setPrefHeight(270);
        bottomSection.setMaxHeight(270);

        VBox content = new VBox(0, header, imageScrollPane, bottomSection);
        VBox.setVgrow(imageScrollPane, Priority.ALWAYS);
        content.setStyle("-fx-background-color: #f7f9fc;");

        BorderPane root = new BorderPane(content);

        uploadBtn.setOnAction(e -> uploadCaseFolder(stage));
        processBtn.setOnAction(e -> processCase(processBtn));
        extractFeaturesBtn.setOnAction(e -> extractFeatures());
        clearBtn.setOnAction(e -> clearAll());

        suspiciousListView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                showPathToSuspiciousNode(newItem.getNode());
            }
        });

        return new Scene(root, 1450, 820);
    }

    private VBox createImageCard(String title, ImageView imageView, Label placeholder) {
        Label cardTitle = new Label(title);
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        cardTitle.setTextFill(Color.web("#1f2937"));

        imageView.setPreserveRatio(false);
        imageView.setFitWidth(330);
        imageView.setFitHeight(280);
        imageView.setSmooth(true);

        placeholder.setFont(Font.font("Segoe UI", 13));
        placeholder.setTextFill(Color.web("#94a3b8"));

        StackPane imageArea = new StackPane(imageView, placeholder);
        imageArea.setAlignment(Pos.CENTER);
        imageArea.setMinHeight(280);
        imageArea.setPrefHeight(280);
        imageArea.setMinWidth(330);
        imageArea.setPrefWidth(330);
        imageArea.setMaxWidth(330);
        imageArea.setStyle(
                "-fx-background-color: #f9fbfd;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #dbe3ea;" +
                "-fx-border-radius: 10;"
        );

        VBox card = new VBox(12, cardTitle, imageArea);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #dbe3ea;" +
                "-fx-border-radius: 12;"
        );

        return card;
    }

    private VBox createSuspiciousListCard() {
        Label title = new Label("Suspicious Nodes");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#1f2937"));

        suspiciousListView.setPrefWidth(360);
        suspiciousListView.setPrefHeight(280);
        suspiciousListView.setPlaceholder(new Label("No suspicious nodes yet"));

        VBox card = new VBox(12, title, suspiciousListView);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #dbe3ea;" +
                "-fx-border-radius: 12;"
        );

        return card;
    }

    private Label createKeyLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#334155"));
        label.setMinWidth(190);
        return label;
    }

    private Label createValueLabel(Label label) {
        label.setFont(Font.font("Segoe UI", 13));
        label.setTextFill(Color.web("#111827"));
        label.setWrapText(true);
        return label;
    }

    private void stylePrimaryButton(Button button) {
        styleButton(button, "#4f86f7", "#3b76eb");
    }

    private void styleSuccessButton(Button button) {
        styleButton(button, "#22c55e", "#16a34a");
    }

    private void styleDangerButton(Button button) {
        styleButton(button, "#ef4444", "#dc2626");
    }

    private void styleButton(Button button, String normalColor, String hoverColor) {
        String baseStyle =
                "-fx-background-color: " + normalColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 18 10 18;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";

        String hoverStyle =
                "-fx-background-color: " + hoverColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 18 10 18;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;";

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    private void uploadCaseFolder(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select CT Case Folder");

        File folder = chooser.showDialog(stage);

        if (folder != null) {
            selectedCaseFolder = folder;
            latestOutputDir = null;
            latestCompressedAirwayImage = null;

            originalImageView.setImage(null);
            finalImageView.setImage(null);
            graphImageView.setImage(null);
            edgeImageView.setImage(null);

            originalPlaceholder.setVisible(true);
            finalPlaceholder.setVisible(true);
            edgePlaceholder.setVisible(true);
            graphPlaceholder.setVisible(true);

            affectedBranchValue.setText(folder.getName());
            narrowingValue.setText("—");
            pathTraceValue.setText("—");
            conditionValue.setText("Ready for conversion");

            topSuspiciousNodeValue.setText("—");
            topReasonValue.setText("—");

            resetFeatureSection();
            resetSimilaritySection();
            clearSuspiciousSection();

            extractFeaturesBtn.setDisable(true);

            statusLabel.setText("Status: Case folder selected successfully. Click Process Case.");
        }
    }

    private void processCase(Button processBtn) {
        if (selectedCaseFolder == null) {
            statusLabel.setText("Status: Please upload a case folder first.");
            return;
        }

        Path caseDir = selectedCaseFolder.toPath();
        Path parent = caseDir.getParent();

        if (parent == null) {
            statusLabel.setText("Status: Invalid selected folder.");
            return;
        }

        Path outputRoot = parent.resolve("output");

        Task<ConvertCT.ConversionResult> task = new Task<>() {
            @Override
            protected ConvertCT.ConversionResult call() throws Exception {
                return ConvertCT.convertCaseFromFolder(
                        caseDir,
                        outputRoot,
                        true,
                        -1000.0,
                        400.0
                );
            }
        };

        processBtn.setDisable(true);
        extractFeaturesBtn.setDisable(true);
        statusLabel.setText("Status: Processing case folder...");

        task.setOnSucceeded(e -> {
            ConvertCT.ConversionResult result = task.getValue();
            latestOutputDir = result.outputDir;

            affectedBranchValue.setText(selectedCaseFolder.getName());
            narrowingValue.setText(String.valueOf(result.savedCount));
            pathTraceValue.setText(result.outputDir.toString());
            conditionValue.setText("Conversion complete. Starting compression...");

            loadPreviewImages(result.outputDir);
            statusLabel.setText("Status: Conversion completed successfully.");

            compressCase(processBtn);
        });

        task.setOnFailed(e -> {
            processBtn.setDisable(false);
            Throwable ex = task.getException();
            statusLabel.setText("Status: Error - " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void compressCase(Button processBtn) {
        if (latestOutputDir == null) {
            statusLabel.setText("Status: Please process the case first before compressing.");
            processBtn.setDisable(false);
            return;
        }

        Task<PngCaseCompressor.CompressionResult> task = new Task<>() {
            @Override
            protected PngCaseCompressor.CompressionResult call() throws Exception {
                return PngCaseCompressor.compressAirwayMasks(latestOutputDir);
            }
        };

        statusLabel.setText("Status: Compressing PNG slices into one final image...");

        task.setOnSucceeded(e -> {
            processBtn.setDisable(false);

            PngCaseCompressor.CompressionResult result = task.getValue();
            latestCompressedAirwayImage = result.getFinalImage();

            AirwayImage finalAirwayImage = result.getFinalImage();
            finalImageView.setImage(finalAirwayImage.displayPixels());
            finalPlaceholder.setVisible(false);

            ImageFilter finalGreyScale = new ImageFilter(finalAirwayImage);
            finalGreyScale.applyGrayScale();

            EdgeDetection finalEdge = new EdgeDetection(finalGreyScale.getGreyScaleImage(), 1);
            edgeImageView.setImage(finalEdge.getEdgeImage().displayPixels());
            edgePlaceholder.setVisible(false);

            ImageFilter maskFilter = new ImageFilter(finalAirwayImage);
            maskFilter.setThreshold(1);
            maskFilter.applyMask();

            int[][] binaryMask = maskFilter.getBinaryMask();

            GraphBuild builder = new GraphBuild(binaryMask);
            Graph graph = builder.buildGraph();

            PathFind pathFinder = new PathFind(graph);
            AirwayNode startNode = pathFinder.findStartNode(graph);
            AirwayNode targetNode = pathFinder.findDeepestEndNodeDifferentFrom(startNode);

            List<AirwayNode> shortestPath = pathFinder.dijkstra(startNode, targetNode);

            SuspiciousNodeDectector susDetect = new SuspiciousNodeDectector();

            Map<AirwayNode, Double> fullCurvatureScores =
                    susDetect.computeFullGraphCurvatureScores(graph);

            Map<AirwayNode, Double> fullNarrowingScores =
                    susDetect.computeFullGraphNarrowingScores(graph);

            Map<AirwayNode, Double> combinedScores =
                    susDetect.combineFullGraphScores(
                            graph,
                            fullCurvatureScores,
                            fullNarrowingScores,
                            0.4,
                            0.6
                    );

            List<AirwayNode> suspiciousNodes =
                    susDetect.findSuspiciousNodes(combinedScores, 0.30);

            List<GraphOverlay.OverlayLine> pathLines = buildPathLines(
                    shortestPath,
                    finalImageView.getImage().getWidth(),
                    finalImageView.getImage().getHeight(),
                    binaryMask.length,
                    binaryMask[0].length
            );

            GraphOverlay.OverlayData overlayData = GraphOverlay.buildOverlayData(
                    graph,
                    finalImageView.getImage().getWidth(),
                    finalImageView.getImage().getHeight(),
                    binaryMask.length,
                    binaryMask[0].length
            );

            Image overlayResult = createOverlayedImage(
                    finalImageView.getImage(),
                    overlayData,
                    pathLines,
                    suspiciousNodes,
                    combinedScores,
                    null,
                    binaryMask.length,
                    binaryMask[0].length
            );

            graphImageView.setImage(overlayResult);
            graphPlaceholder.setVisible(false);

            currentPathFinder = pathFinder;
            currentStartNode = startNode;
            currentBaseImage = finalImageView.getImage();
            currentOverlayData = overlayData;
            currentMaskRows = binaryMask.length;
            currentMaskCols = binaryMask[0].length;
            currentCombinedScores = combinedScores;
            currentSuspiciousNodes = suspiciousNodes;

            List<SuspiciousItem> suspiciousItems = new ArrayList<>();
            for (AirwayNode node : suspiciousNodes) {
                double score = combinedScores.getOrDefault(node, 0.0);

                String reason = susDetect.buildFullGraphReason(
                        node,
                        fullCurvatureScores,
                        fullNarrowingScores,
                        0.20,
                        0.35
                );

                suspiciousItems.add(new SuspiciousItem(node, score, reason));
            }

            suspiciousItems.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            suspiciousListView.getItems().setAll(suspiciousItems);

            if (!suspiciousItems.isEmpty()) {
                SuspiciousItem top = suspiciousItems.get(0);

                topSuspiciousNodeValue.setText(
                        "row=" + top.getNode().getRow()
                                + ", col=" + top.getNode().getCol()
                                + " | score=" + String.format("%.3f", top.getScore())
                );
                topReasonValue.setText(top.getReason());
            } else {
                topSuspiciousNodeValue.setText("None");
                topReasonValue.setText("No suspicious nodes detected");
            }

            conditionValue.setText("Compression complete. Click Extract Features.");
            statusLabel.setText(
                    "Status: Final compressed airway image created. Suspicious nodes found: " + suspiciousNodes.size()
            );

            extractFeaturesBtn.setDisable(false);
        });

        task.setOnFailed(e -> {
            processBtn.setDisable(false);
            Throwable ex = task.getException();
            statusLabel.setText("Status: Compression error - " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void extractFeatures() {
        if (latestCompressedAirwayImage == null) {
            statusLabel.setText("Status: Please process and compress the case first.");
            return;
        }

        extractFeaturesBtn.setDisable(true);
        statusLabel.setText("Status: Extracting features...");

        Task<FeatureTaskResult> task = new Task<>() {
            @Override
            protected FeatureTaskResult call() throws Exception {
                updateMessage("Status: Extracting structural airway features from final compressed image...");

                PipelineResult pipelineResult =
                        SimilarityFeatureExtractor.runFullPipeline(latestCompressedAirwayImage);

                ComparisonResult comparisonResult = null;
                String similarityError = null;

                try {
                    updateMessage("Status: Running KNN similarity comparison...");
                    Path datasetFeaturePath = resolveDatasetFeaturesPath();

                    comparisonResult = SimilarityFeatureExtractor.compareWithDataset(
                            pipelineResult.getFeatures(),
                            datasetFeaturePath,
                            3
                    );
                } catch (Exception ex) {
                    similarityError = ex.getMessage();
                }

                return new FeatureTaskResult(pipelineResult, comparisonResult, similarityError);
            }
        };

        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            extractFeaturesBtn.setDisable(false);

            FeatureTaskResult result = task.getValue();

            updateFeatureSection(result.getPipelineResult().getFeatures());

            if (result.getComparisonResult() != null) {
                updateSimilaritySection(result.getComparisonResult());
                conditionValue.setText("Feature extraction and similarity comparison complete.");
                statusLabel.setText("Status: Structural airway features and KNN similarity extracted successfully.");
            } else {
                resetSimilaritySection();
                similarityExplanationLabel.setText(
                        "The structural features were extracted successfully, but the KNN comparison could not be completed."
                                + (result.getSimilarityError() == null ? "" : " Reason: " + result.getSimilarityError())
                );
                conditionValue.setText("Feature extraction complete. Similarity comparison not completed.");
                statusLabel.setText("Status: Features extracted successfully, but similarity comparison failed.");
            }
        });

        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            extractFeaturesBtn.setDisable(false);

            Throwable ex = task.getException();
            statusLabel.setText("Status: Feature extraction error - " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void updateFeatureSection(SimilarityFeatures features) {
        branchCountValue.setText(features.getBranchCount() + " airway segments found in the graph");
        branchPointCountValue.setText(features.getBranchPointCount() + " branch-point nodes found in the graph");
        averageTaperingValue.setText(formatDecimal(features.getAverageTapering()) + " average tapering across airway segments");
        averageTortuosityValue.setText(formatDecimal(features.getAverageTortuosity()) + " average tortuosity across airway segments");
        abruptEndingRatioValue.setText(formatDecimal(features.getAbruptEndingRatio()) + " abrupt-ending ratio");

        extractedFeatureExplanationLabel.setText(
                "Features extracted successfully from the final compressed image using the binary mask and graph construction."
        );
    }

    private void updateSimilaritySection(ComparisonResult comparisonResult) {
        knnValue.setText(String.valueOf(comparisonResult.getKUsed()));
        bestMatchCaseValue.setText("Case " + comparisonResult.getBestMatch().getDatasetCase().getCaseId());
        bestMatchPercentageValue.setText(String.format("%.2f%%", comparisonResult.getBestMatch().getSimilarityPercentage()));
        nearestCasesValue.setText(comparisonResult.buildNeighborSummary());

        similarityExplanationLabel.setText(
                "Through the KNN comparison, the uploaded case is most similar to Case "
                        + comparisonResult.getBestMatch().getDatasetCase().getCaseId()
                        + " with a similarity score of "
                        + String.format("%.2f%%", comparisonResult.getBestMatch().getSimilarityPercentage())
                        + ". "
                        + comparisonResult.getBestMatch().getFeatureExplanation()
        );
    }

    private Path resolveDatasetFeaturesPath() throws IOException {
        Path srcPath = Paths.get("src", "dataset_features.txt");
        if (Files.exists(srcPath)) {
            return srcPath;
        }

        Path rootPath = Paths.get("dataset_features.txt");
        if (Files.exists(rootPath)) {
            return rootPath;
        }

        throw new IOException("dataset_features.txt was not found in src or project root.");
    }

    private List<GraphOverlay.OverlayLine> buildPathLines(
            List<AirwayNode> path,
            double imageWidth,
            double imageHeight,
            int maskRows,
            int maskCols
    ) {
        List<GraphOverlay.OverlayLine> pathLines = new ArrayList<>();

        if (path == null || path.size() < 2) {
            return pathLines;
        }

        double scaleX = imageWidth / maskCols;
        double scaleY = imageHeight / maskRows;

        for (int i = 0; i < path.size() - 1; i++) {
            AirwayNode from = path.get(i);
            AirwayNode to = path.get(i + 1);

            double x1 = from.getCol() * scaleX;
            double y1 = from.getRow() * scaleY;
            double x2 = to.getCol() * scaleX;
            double y2 = to.getRow() * scaleY;

            pathLines.add(new GraphOverlay.OverlayLine(x1, y1, x2, y2));
        }

        return pathLines;
    }

    private void loadPreviewImages(Path outputDir) {
        Path ctDir = outputDir.resolve("ct_coronal_png");
        Image ctImage = loadFirstPng(ctDir);

        originalImageView.setImage(ctImage);
        originalPlaceholder.setVisible(ctImage == null);
    }

    private Image loadFirstPng(Path folder) {
        if (folder == null || !Files.exists(folder) || !Files.isDirectory(folder)) {
            return null;
        }

        try (Stream<Path> stream = Files.list(folder)) {
            Optional<Path> first = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .findFirst();

            if (first.isPresent()) {
                return new Image(first.get().toUri().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void resetFeatureSection() {
        branchCountValue.setText("—");
        branchPointCountValue.setText("—");
        averageTaperingValue.setText("—");
        averageTortuosityValue.setText("—");
        abruptEndingRatioValue.setText("—");
        extractedFeatureExplanationLabel.setText("No structural features extracted yet.");
    }

    private void resetSimilaritySection() {
        knnValue.setText("—");
        bestMatchCaseValue.setText("—");
        bestMatchPercentageValue.setText("—");
        nearestCasesValue.setText("—");
        similarityExplanationLabel.setText("No similarity comparison has been performed yet.");
    }

    private void clearSuspiciousSection() {
        topSuspiciousNodeValue.setText("—");
        topReasonValue.setText("—");
        suspiciousListView.getItems().clear();

        currentPathFinder = null;
        currentStartNode = null;
        currentBaseImage = null;
        currentOverlayData = null;
        currentMaskRows = 0;
        currentMaskCols = 0;

        currentCombinedScores = new HashMap<>();
        currentSuspiciousNodes = new ArrayList<>();
    }

    private void clearAll() {
        selectedCaseFolder = null;
        latestOutputDir = null;
        latestCompressedAirwayImage = null;

        originalImageView.setImage(null);
        finalImageView.setImage(null);
        graphImageView.setImage(null);
        edgeImageView.setImage(null);

        originalPlaceholder.setVisible(true);
        finalPlaceholder.setVisible(true);
        edgePlaceholder.setVisible(true);
        graphPlaceholder.setVisible(true);

        affectedBranchValue.setText("—");
        narrowingValue.setText("—");
        pathTraceValue.setText("—");
        conditionValue.setText("—");

        resetFeatureSection();
        resetSimilaritySection();
        clearSuspiciousSection();

        extractFeaturesBtn.setDisable(true);
        statusLabel.setText("Status: Cleared. Waiting for case folder upload...");
    }

    private Image createOverlayedImage(
            Image baseImage,
            GraphOverlay.OverlayData overlayData,
            List<GraphOverlay.OverlayLine> pathLines,
            List<AirwayNode> suspiciousNodes,
            Map<AirwayNode, Double> combinedScores,
            AirwayNode selectedSuspiciousNode,
            int maskRows,
            int maskCols
    ) {
        double width = baseImage.getWidth();
        double height = baseImage.getHeight();

        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(width, height);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.drawImage(baseImage, 0, 0, width, height);

        gc.setStroke(javafx.scene.paint.Color.RED);
        gc.setLineWidth(2.8);
        for (GraphOverlay.OverlayLine line : overlayData.getLines()) {
            gc.strokeLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
        }

        for (GraphOverlay.OverlayNode node : overlayData.getNodes()) {
            switch (node.getType()) {
                case START:
                    gc.setFill(javafx.scene.paint.Color.BLUE);
                    break;
                case END:
                    gc.setFill(javafx.scene.paint.Color.LIMEGREEN);
                    break;
                case BRANCH:
                    gc.setFill(javafx.scene.paint.Color.YELLOW);
                    break;
                default:
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    break;
            }

            double radius = 4.5;
            gc.fillOval(node.getX() - radius, node.getY() - radius, radius * 2, radius * 2);
        }

        double scaleX = width / maskCols;
        double scaleY = height / maskRows;

        gc.setFill(Color.web("#27D3F5"));
        for (AirwayNode node : suspiciousNodes) {
            double score = combinedScores.getOrDefault(node, 0.0);
            double x = node.getCol() * scaleX;
            double y = node.getRow() * scaleY;

            double diameter = 9.0 + (score * 8.0);
            gc.fillOval(x - diameter / 2.0, y - diameter / 2.0, diameter, diameter);
        }

        gc.setStroke(javafx.scene.paint.Color.YELLOW);
        gc.setLineWidth(6.0);
        for (GraphOverlay.OverlayLine line : pathLines) {
            gc.strokeLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
        }

        if (selectedSuspiciousNode != null) {
            double x = selectedSuspiciousNode.getCol() * scaleX;
            double y = selectedSuspiciousNode.getRow() * scaleY;

            gc.setFill(Color.web("#2310AD"));
            gc.fillOval(x - 8, y - 8, 16, 16);
        }

        WritableImage result = new WritableImage((int) Math.ceil(width), (int) Math.ceil(height));
        return canvas.snapshot(new SnapshotParameters(), result);
    }

    private void showPathToSuspiciousNode(AirwayNode suspiciousNode) {
        if (currentPathFinder == null
                || currentStartNode == null
                || suspiciousNode == null
                || currentBaseImage == null
                || currentOverlayData == null) {
            return;
        }

        List<AirwayNode> reroutedPath = currentPathFinder.dijkstra(currentStartNode, suspiciousNode);

        List<GraphOverlay.OverlayLine> reroutedLines = buildPathLines(
                reroutedPath,
                currentBaseImage.getWidth(),
                currentBaseImage.getHeight(),
                currentMaskRows,
                currentMaskCols
        );

        Image reroutedImage = createOverlayedImage(
                currentBaseImage,
                currentOverlayData,
                reroutedLines,
                currentSuspiciousNodes,
                currentCombinedScores,
                suspiciousNode,
                currentMaskRows,
                currentMaskCols
        );

        graphImageView.setImage(reroutedImage);
        statusLabel.setText(
                "Status: Showing shortest path to suspicious node at row="
                        + suspiciousNode.getRow()
                        + ", col="
                        + suspiciousNode.getCol()
        );
    }

    private String formatDecimal(double value) {
        return String.format("%.6f", value);
    }
}