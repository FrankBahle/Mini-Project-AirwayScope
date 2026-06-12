package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import DataStructure.ArrayList;
import java.util.Comparator;
import DataStructure.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import GraphConstruction.AirwayNode;
import GraphConstruction.Graph;
import GraphConstruction.GraphOverlay;
import PathFindingDectection.PathFind;
import SimilarityDetection.SuspiciousNodeAnalyzer;
import SimilarityDetection.SuspiciousNodeRecord;
import SimilarityDetection.ComparisonResult;
import SimilarityDetection.NeighborMatch;
import SimilarityDetection.PipelineResult;
import SimilarityDetection.SimilarityFeatureExtractor;
import SimilarityDetection.SimilarityFeatures;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import javafx.util.Duration;
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

    private final Label topSuspiciousNodeValue = new Label("—");
    private final Label topReasonValue = new Label("—");
    private final ListView<SuspiciousItem> suspiciousListView = new ListView<>();

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

    private final Label statusLabel = new Label("Waiting for case folder upload...");
    private final StackPane loadingOverlay = new StackPane();
    private final StackPane featureGuideOverlay = new StackPane();
    private final Label loadingLabel = new Label("Processing CT case...");
    private final Label loadingInfoLabel = new Label();
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final GaussianBlur busyBlur = new GaussianBlur(16);
    private Timeline loadingInfoTimeline;

    private File selectedCaseFolder;
    private Path latestOutputDir;
    private AirwayImage latestCompressedAirwayImage;
    private PipelineResult currentPipelineResult;

    private Button extractFeaturesBtn;
    private VBox mainContent;

    private PathFind currentPathFinder;
    private AirwayNode currentStartNode;
    private Image currentBaseImage;
    private GraphOverlay.OverlayData currentOverlayData;
    private int currentMaskRows;
    private int currentMaskCols;

    private Map<AirwayNode, Double> currentCombinedScores = new HashMap<>();
    private List<AirwayNode> currentSuspiciousNodes = new ArrayList<>();
    private int currentSelectedSuspiciousNumber = -1;

    private static class SuspiciousItem {
        private final int nodeNumber;
        private final AirwayNode node;
        private final double score;
        private final String reason;

        public SuspiciousItem(int nodeNumber, AirwayNode node, double score, String reason) {
            this.nodeNumber = nodeNumber;
            this.node = node;
            this.score = score;
            this.reason = reason;
        }

        public int getNodeNumber() { return nodeNumber; }
        public AirwayNode getNode() { return node; }
        public double getScore() { return score; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return "Node " + nodeNumber + " | score=" + String.format("%.3f", score) + " | " + reason;
        }
    }

    private static class FeatureTaskResult {
        private final PipelineResult pipelineResult;
        private final ComparisonResult comparisonResult;
        private final String similarityError;

        public FeatureTaskResult(PipelineResult pr, ComparisonResult cr, String se) {
            this.pipelineResult = pr;
            this.comparisonResult = cr;
            this.similarityError = se;
        }

        public PipelineResult getPipelineResult() { return pipelineResult; }
        public ComparisonResult getComparisonResult() { return comparisonResult; }
        public String getSimilarityError() { return similarityError; }
    }

    public Scene createScene(Stage stage) {
        Label titleLabel = new Label("Airway Analysis System");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("Convert CT case folders to PNG slices, then compress them into one airway image");
        subtitleLabel.setFont(Font.font("System", 13));
        subtitleLabel.setTextFill(Color.web("#e2e8f0"));
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(420);

        VBox titleBox = new VBox(4, titleLabel, subtitleLabel);

        styleStatusBadge();
        statusLabel.setMinWidth(330);
        statusLabel.setMaxWidth(430);

        Button uploadBtn = new Button("Upload Case Folder");
        Button processBtn = new Button("Process Case");
        extractFeaturesBtn = new Button("Extract Features");
        Button clearBtn = new Button("Clear");

        stylePrimaryButton(uploadBtn);
        styleSuccessButton(processBtn);
        stylePrimaryButton(extractFeaturesBtn);
        styleDangerButton(clearBtn);

        extractFeaturesBtn.setDisable(true);

        HBox buttonBox = new HBox(12, uploadBtn, processBtn, extractFeaturesBtn, clearBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusContainer = new HBox(statusLabel);
        statusContainer.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(20, titleBox, statusContainer, spacer, buttonBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 28, 20, 28));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #1e293b, #334155); " +
                "-fx-background-radius: 0;");
        DropShadow headerShadow = new DropShadow();
        headerShadow.setColor(Color.rgb(0,0,0,0.2));
        headerShadow.setRadius(10);
        headerShadow.setOffsetY(2);
        header.setEffect(headerShadow);

        VBox originalCard = createImageCard("CT Preview", originalImageView, originalPlaceholder, 280, 280);
        VBox finalCard   = createImageCard("Final Compressed", finalImageView, finalPlaceholder, 280, 280);
        VBox edgeCard    = createImageCard("Edge Image", edgeImageView, edgePlaceholder, 280, 280);
        VBox graphCard   = createImageCard("Graph Image", graphImageView, graphPlaceholder, 280, 280);

        HBox imageStrip = new HBox(18, originalCard, finalCard, edgeCard, graphCard);
        imageStrip.setPadding(new Insets(24, 20, 16, 20));
        imageStrip.setMinHeight(430);
        imageStrip.setPrefHeight(430);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #f1f5f9; -fx-tab-min-height: 42px; -fx-tab-max-height: 42px;");

        Tab caseTab = new Tab("📁  Case Progress");
        caseTab.setContent(buildCaseProgressTab());
        caseTab.setClosable(false);

        Tab suspiciousTab = new Tab("🔍  Suspicious Nodes");
        suspiciousTab.setContent(buildSuspiciousTab());
        suspiciousTab.setClosable(false);

        Tab featuresTab = new Tab("📊  Extracted Features");
        featuresTab.setContent(buildFeaturesTab());
        featuresTab.setClosable(false);

        Tab similarityTab = new Tab("🔗  Similarity Results");
        similarityTab.setContent(buildSimilarityTab());
        similarityTab.setClosable(false);

        tabPane.getTabs().addAll(caseTab, suspiciousTab, featuresTab, similarityTab);

        mainContent = new VBox(0, header, imageStrip, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        mainContent.setStyle("-fx-background-color: #f1f5f9;");

        BorderPane root = new BorderPane(mainContent);

        configureLoadingOverlay();
        configureFeatureGuideOverlay();
        StackPane rootStack = new StackPane(root, loadingOverlay, featureGuideOverlay);

        uploadBtn.setOnAction(e -> uploadCaseFolder(stage));
        processBtn.setOnAction(e -> processCase(processBtn));
        extractFeaturesBtn.setOnAction(e -> extractFeatures());
        clearBtn.setOnAction(e -> clearAll());

        suspiciousListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> { if (newItem != null) showPathToSuspiciousNode(newItem); });

        Scene scene = new Scene(rootStack, 1450, 820);
        scene.getRoot().setStyle("-fx-font-family: 'System';");

        VBox loadingCard = (VBox) loadingOverlay.getChildren().get(0);
        loadingCard.prefHeightProperty().bind(scene.heightProperty().multiply(0.6));
        loadingCard.maxHeightProperty().bind(scene.heightProperty().multiply(0.6));

        return scene;
    }

    private VBox buildCaseProgressTab() {
        return buildTabCard("Case Progress",
                new String[]{"Selected Case:", "Saved PNG Slices:", "Output Folder:", "Next Step:"},
                new Label[]{affectedBranchValue, narrowingValue, pathTraceValue, conditionValue});
    }

    private ScrollPane buildSuspiciousTab() {
        Label title = new Label("Suspicious Nodes & Pathfinding");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#334155"));

        GridPane infoGrid = gridWithValues(
                new String[]{"Top Suspicious Node:", "Top Reason:"},
                new Label[]{topSuspiciousNodeValue, topReasonValue});

        suspiciousListView.setPrefWidth(600);
        suspiciousListView.setPrefHeight(500);
        suspiciousListView.setPlaceholder(new Label("No suspicious nodes yet"));
        VBox.setVgrow(suspiciousListView, Priority.ALWAYS);

        VBox card = new VBox(18, title, new Separator(), infoGrid, new Separator(), suspiciousListView);
        card.setPadding(new Insets(24));
        styleTabCard(card);

        ScrollPane scrollPane = new ScrollPane(card);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: #f1f5f9; -fx-background-color: #f1f5f9;");
        return scrollPane;
    }

    private VBox buildFeaturesTab() {
        Label title = new Label("Extracted Features");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#334155"));

        GridPane grid = gridWithValues(
                new String[]{"Feature 1: Branch Count", "Feature 2: Branch Point Count",
                        "Feature 3: Average Tapering", "Feature 4: Average Tortuosity",
                        "Feature 5: Abrupt-Ending Ratio"},
                new Label[]{branchCountValue, branchPointCountValue, averageTaperingValue,
                        averageTortuosityValue, abruptEndingRatioValue});

        extractedFeatureExplanationLabel.setWrapText(true);
        extractedFeatureExplanationLabel.setFont(Font.font("System", 13));
        extractedFeatureExplanationLabel.setTextFill(Color.web("#475569"));

        VBox card = new VBox(18, title, new Separator(), grid, new Separator(), extractedFeatureExplanationLabel);
        card.setPadding(new Insets(24));
        return styleTabCard(card);
    }

    private VBox buildSimilarityTab() {
        Label title = new Label("KNN Similarity Results");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#334155"));

        GridPane grid = gridWithValues(
                new String[]{"K Used:", "Most Similar Case:", "Similarity Percentage:", "Nearest Cases:"},
                new Label[]{knnValue, bestMatchCaseValue, bestMatchPercentageValue, nearestCasesValue});

        similarityExplanationLabel.setWrapText(true);
        similarityExplanationLabel.setFont(Font.font("System", 13));
        similarityExplanationLabel.setTextFill(Color.web("#475569"));

        VBox card = new VBox(18, title, new Separator(), grid, new Separator(), similarityExplanationLabel);
        card.setPadding(new Insets(24));
        return styleTabCard(card);
    }

    private VBox buildTabCard(String title, String[] keys, Label[] values) {
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        t.setTextFill(Color.web("#334155"));
        GridPane grid = gridWithValues(keys, values);
        VBox card = new VBox(18, t, new Separator(), grid);
        card.setPadding(new Insets(24));
        return styleTabCard(card);
    }

    private GridPane gridWithValues(String[] keys, Label[] values) {
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(16);
        for (int i = 0; i < keys.length; i++) {
            grid.add(createKeyLabel(keys[i]), 0, i);
            grid.add(createValueLabel(values[i]), 1, i);
        }
        return grid;
    }

    private VBox styleTabCard(VBox card) {
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 16; " +
                "-fx-border-width: 0 0 0 4; -fx-border-color: #3b82f6;");
        card.setEffect(new DropShadow(12, 0, 4, Color.rgb(0,0,0,0.08)));
        return card;
    }

    private void configureLoadingOverlay() {
        loadingIndicator.setPrefSize(50, 50);
        loadingIndicator.setStyle("-fx-progress-color: white;");

        loadingLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        loadingLabel.setTextFill(Color.WHITE);
        loadingLabel.setWrapText(true);
        loadingLabel.setMaxWidth(320);
        loadingLabel.setAlignment(Pos.CENTER);

        loadingInfoLabel.setFont(Font.font("System", 12));
        loadingInfoLabel.setTextFill(Color.web("#cbd5e1"));
        loadingInfoLabel.setWrapText(true);
        loadingInfoLabel.setMaxWidth(380);
        loadingInfoLabel.setAlignment(Pos.CENTER);
        loadingInfoLabel.setText(getLoadingInfoMessages()[0]);

        VBox loadingCard = new VBox(14, loadingIndicator, loadingLabel, loadingInfoLabel);
        loadingCard.setAlignment(Pos.CENTER);
        loadingCard.setPadding(new Insets(24, 32, 24, 32));
        loadingCard.setMaxWidth(420);
        loadingCard.setPrefHeight(160);
        loadingCard.setMaxHeight(160);
        loadingCard.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.75); -fx-background-radius: 20; " +
                "-fx-border-color: rgba(255,255,255,0.18); -fx-border-radius: 20;");
        loadingCard.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.5)));

        loadingOverlay.getChildren().setAll(loadingCard);
        loadingOverlay.setAlignment(Pos.CENTER);
        loadingOverlay.setStyle("-fx-background-color: rgba(241, 245, 249, 0.5);");
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
        loadingOverlay.setMouseTransparent(false);
    }

    private void configureFeatureGuideOverlay() {
        featureGuideOverlay.setAlignment(Pos.CENTER);
        featureGuideOverlay.setStyle("-fx-background-color: rgba(241, 245, 249, 0.5);");
        featureGuideOverlay.setVisible(false);
        featureGuideOverlay.setManaged(false);
        featureGuideOverlay.setMouseTransparent(false);
    }

    private String[] getLoadingInfoMessages() {
        return new String[]{
                "Similarity compares the uploaded airway structure against saved airway cases to find the closest matches.",
                "Pathfinding traces the shortest route through the airway graph from the start node to a suspicious target node.",
                "Suspicious-node analysis checks airway narrowing and curvature to mark parts of the airway that may need attention."
        };
    }

    private void startLoadingInfoRotation() {
        String[] messages = getLoadingInfoMessages();
        loadingInfoLabel.setText(messages[0]);
        if (loadingInfoTimeline != null) loadingInfoTimeline.stop();
        final int[] index = {0};
        loadingInfoTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            index[0] = (index[0] + 1) % messages.length;
            loadingInfoLabel.setText(messages[index[0]]);
        }));
        loadingInfoTimeline.setCycleCount(Timeline.INDEFINITE);
        loadingInfoTimeline.play();
    }

    private void stopLoadingInfoRotation() {
        if (loadingInfoTimeline != null) loadingInfoTimeline.stop();
    }

    private void showLoading(String message) {
        loadingLabel.setText(message);
        startLoadingInfoRotation();
        if (mainContent != null) {
            mainContent.setEffect(busyBlur);
            mainContent.setDisable(true);
        }
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
        updateStatusStyle(message);
    }

    private void updateLoadingMessage(String message) {
        loadingLabel.setText(message);
        updateStatusStyle(message);
    }

    private void hideLoading() {
        stopLoadingInfoRotation();
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
        if (mainContent != null) {
            mainContent.setEffect(null);
            mainContent.setDisable(false);
        }
    }

    private void showFeatureGuidePopup(SimilarityFeatures f) {
        // Main title section
        Label titleIcon = new Label("📊");
        titleIcon.setFont(Font.font("System", 32));
        
        Label titleText = new Label("Extracted Feature Guide");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleText.setTextFill(Color.WHITE);
        
        HBox titleBox = new HBox(12, titleIcon, titleText);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label subtitleLabel = new Label("Understanding Your Airway Features");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.setTextFill(Color.web("#a5b4fc"));
        subtitleLabel.setPadding(new Insets(0, 0, 8, 0));

        Label intro = new Label(
            "This guide explains each extracted airway feature in plain language. " +
            "The table below shows your measured values and what they typically mean for airway health."
        );
        intro.setFont(Font.font("System", 13));
        intro.setTextFill(Color.web("#cbd5e1"));
        intro.setWrapText(true);
        intro.setMaxWidth(900);
        intro.setPadding(new Insets(0, 0, 16, 0));

        // Create beautiful table
        GridPane table = new GridPane();
        table.setHgap(0);
        table.setVgap(0);
        table.setMaxWidth(960);
        table.setStyle("-fx-background-radius: 12; -fx-border-radius: 12;");

        // Header row with gradient background
        addBeautifulTableHeader(table, 0, "FEATURE", "YOUR VALUE", "NORMAL RANGE", "INTERPRETATION");

        // Data rows with alternating colors and icons
        addBeautifulTableRow(table, 1, 
            "🌿 Branch Count", 
            f.getBranchCount() + " branches", 
            "100 - 200", 
            interpretBranchCount(f.getBranchCount()), 
            getBranchCountStatus(f.getBranchCount()));
        
        addBeautifulTableRow(table, 2, 
            "🔀 Branch Point Count", 
            f.getBranchPointCount() + " branch points", 
            "60 - 120", 
            interpretBranchPointCount(f.getBranchPointCount()), 
            getBranchPointCountStatus(f.getBranchPointCount()));
        
        addBeautifulTableRow(table, 3, 
            "📉 Average Tapering", 
            formatDecimal(f.getAverageTapering()), 
            "< 0.03", 
            interpretTapering(f.getAverageTapering()), 
            getTaperingStatus(f.getAverageTapering()));
        
        addBeautifulTableRow(table, 4, 
            "🔄 Average Tortuosity", 
            formatDecimal(f.getAverageTortuosity()), 
            "< 1.2", 
            interpretTortuosity(f.getAverageTortuosity()), 
            getTortuosityStatus(f.getAverageTortuosity()));
        
        addBeautifulTableRow(table, 5, 
            "⚠ Abrupt-Ending Ratio", 
            formatDecimal(f.getAbruptEndingRatio()), 
            "< 0.2", 
            interpretAbruptEnding(f.getAbruptEndingRatio()), 
            getAbruptEndingStatus(f.getAbruptEndingRatio()));

        // Summary card at bottom
        VBox summaryCard = new VBox(8);
        summaryCard.setStyle(
            "-fx-background-color: rgba(59, 130, 246, 0.15); " +
            "-fx-background-radius: 12; " +
            "-fx-border-color: rgba(59, 130, 246, 0.3); " +
            "-fx-border-radius: 12;"
        );
        summaryCard.setPadding(new Insets(16, 20, 16, 20));
        
        Label summaryTitle = new Label("💡 Quick Summary");
        summaryTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        summaryTitle.setTextFill(Color.web("#60a5fa"));
        
        Label summaryText = new Label(generateQuickSummary(f));
        summaryText.setFont(Font.font("System", 12));
        summaryText.setTextFill(Color.web("#e2e8f0"));
        summaryText.setWrapText(true);
        
        summaryCard.getChildren().addAll(summaryTitle, summaryText);

        // Close button with better styling
        Button closeBtn = new Button("✓  Close Guide");
        styleCloseButton(closeBtn);
        closeBtn.setOnAction(e -> hideFeatureGuidePopup());

        HBox closeBox = new HBox(closeBtn);
        closeBox.setAlignment(Pos.CENTER_RIGHT);
        closeBox.setPadding(new Insets(16, 0, 0, 0));

        // Main content container
        VBox contentBox = new VBox(8, titleBox, subtitleLabel, intro, table, summaryCard, closeBox);
        contentBox.setPadding(new Insets(26, 32, 26, 32));
        contentBox.setMaxWidth(1020);
        contentBox.setMaxHeight(650);
        
        ScrollPane scrollContent = new ScrollPane(contentBox);
        scrollContent.setFitToWidth(true);
        scrollContent.setFitToHeight(true);
        scrollContent.setStyle(
            "-fx-background: transparent; " +
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent;"
        );
        scrollContent.getStyleClass().add("edge-to-edge");

        VBox card = new VBox(scrollContent);
        card.setStyle(
            "-fx-background-color: rgba(15, 23, 42, 0.92); " +
            "-fx-background-radius: 24; " +
            "-fx-border-color: rgba(59, 130, 246, 0.4); " +
            "-fx-border-width: 1.5; " +
            "-fx-border-radius: 24;"
        );
        card.setEffect(new DropShadow(25, 0, 8, Color.rgb(0, 0, 0, 0.6)));
        card.setMaxWidth(1060);
        card.setMaxHeight(700);

        featureGuideOverlay.getChildren().setAll(card);

        if (mainContent != null) {
            mainContent.setEffect(busyBlur);
            mainContent.setDisable(true);
        }

        featureGuideOverlay.setVisible(true);
        featureGuideOverlay.setManaged(true);
    }

    private void addBeautifulTableHeader(GridPane table, int row, String c1, String c2, String c3, String c4) {
        Label h1 = createBeautifulHeaderCell(c1);
        Label h2 = createBeautifulHeaderCell(c2);
        Label h3 = createBeautifulHeaderCell(c3);
        Label h4 = createBeautifulHeaderCell(c4);
        
        table.add(h1, 0, row);
        table.add(h2, 1, row);
        table.add(h3, 2, row);
        table.add(h4, 3, row);
    }

    private void addBeautifulTableRow(GridPane table, int row, String feature, String value, String normalRange, String interpretation, String statusEmoji) {
        Label featureCell = createBeautifulBodyCell(feature, true);
        Label valueCell = createBeautifulBodyCell(value + " " + getStatusIcon(statusEmoji), false);
        Label rangeCell = createBeautifulBodyCell(normalRange, false);
        Label interpCell = createBeautifulBodyCell(interpretation, false);
        
        // Add background color for alternating rows
        String bgColor = (row % 2 == 0) ? "rgba(30, 41, 59, 0.92)" : "rgba(51, 65, 85, 0.88)";
        featureCell.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 12 10 12 10; -fx-border-color: rgba(255,255,255,0.08);");
        valueCell.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 12 10 12 10; -fx-border-color: rgba(255,255,255,0.08);");
        rangeCell.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 12 10 12 10; -fx-border-color: rgba(255,255,255,0.08);");
        interpCell.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 12 10 12 10; -fx-border-color: rgba(255,255,255,0.08);");
        
        table.add(featureCell, 0, row);
        table.add(valueCell, 1, row);
        table.add(rangeCell, 2, row);
        table.add(interpCell, 3, row);
    }

    private Label createBeautifulHeaderCell(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setTextFill(Color.WHITE);
        label.setWrapText(true);
        label.setMinWidth(160);
        label.setMaxWidth(280);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPadding(new Insets(14, 10, 14, 10));
        label.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #3b82f6, #2563eb); " +
            "-fx-border-color: rgba(255,255,255,0.15); " +
            "-fx-background-radius: 0;"
        );
        return label;
    }

    private Label createBeautifulBodyCell(String text, boolean bold) {
        Label label = new Label(text);
        if (bold) {
            label.setFont(Font.font("System", FontWeight.BOLD, 12));
        } else {
            label.setFont(Font.font("System", 12));
        }
        label.setTextFill(Color.web("#e2e8f0"));
        label.setWrapText(true);
        label.setMinWidth(160);
        label.setMaxWidth(280);
        label.setPadding(new Insets(12, 10, 12, 10));
        return label;
    }

    private void styleCloseButton(Button btn) {
        btn.setFont(Font.font("System", FontWeight.BOLD, 13));
        btn.setTextFill(Color.WHITE);
        btn.setPadding(new Insets(10, 24, 10, 24));
        btn.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #3b82f6, #2563eb); " +
            "-fx-background-radius: 100; " +
            "-fx-cursor: hand;"
        );
        DropShadow shadow = new DropShadow(8, 2, 2, Color.rgb(0, 0, 0, 0.3));
        btn.setEffect(shadow);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #60a5fa, #3b82f6); " +
                "-fx-background-radius: 100; -fx-cursor: hand;"
            );
            btn.setEffect(new DropShadow(12, 3, 3, Color.rgb(0, 0, 0, 0.4)));
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3b82f6, #2563eb); " +
                "-fx-background-radius: 100; -fx-cursor: hand;"
            );
            btn.setEffect(shadow);
        });
    }

    private String getStatusIcon(String status) {
        if (status.contains("✅")) return "✅";
        if (status.contains("⚠")) return "⚠";
        if (status.contains("❌")) return "❌";
        return "ℹ";
    }

    private String getBranchCountStatus(int value) {
        if (value < 50) return "❌";
        if (value < 100) return "⚠";
        if (value <= 200) return "✅";
        return "⚠";
    }

    private String getBranchPointCountStatus(int value) {
        if (value < 30) return "❌";
        if (value < 60) return "⚠";
        if (value <= 120) return "✅";
        return "⚠";
    }

    private String getTaperingStatus(double value) {
        if (value <= 0.03) return "✅";
        if (value <= 0.05) return "⚠";
        return "⚠";
    }

    private String getTortuosityStatus(double value) {
        if (value <= 1.2) return "✅";
        if (value <= 1.3) return "⚠";
        return "⚠";
    }

    private String getAbruptEndingStatus(double value) {
        if (value <= 0.2) return "✅";
        if (value <= 0.3) return "⚠";
        return "⚠";
    }

    private String generateQuickSummary(SimilarityFeatures f) {
        int goodCount = 0;
        int warningCount = 0;
        int badCount = 0;
        
        if (f.getBranchCount() >= 100 && f.getBranchCount() <= 200) goodCount++;
        else if (f.getBranchCount() < 100) warningCount++;
        else warningCount++;
        
        if (f.getBranchPointCount() >= 60 && f.getBranchPointCount() <= 120) goodCount++;
        else if (f.getBranchPointCount() < 60) warningCount++;
        else warningCount++;
        
        if (f.getAverageTapering() <= 0.03) goodCount++;
        else if (f.getAverageTapering() <= 0.05) warningCount++;
        else badCount++;
        
        if (f.getAverageTortuosity() <= 1.2) goodCount++;
        else if (f.getAverageTortuosity() <= 1.3) warningCount++;
        else badCount++;
        
        if (f.getAbruptEndingRatio() <= 0.2) goodCount++;
        else if (f.getAbruptEndingRatio() <= 0.3) warningCount++;
        else badCount++;
        
        if (badCount >= 2) {
            return "⚠ Multiple features fall into concerning ranges. Consider reviewing this airway case for potential abnormalities.";
        } else if (warningCount >= 2) {
            return "📊 Some features show mild deviations from normal ranges. Recommend monitoring for clinical context.";
        } else if (goodCount >= 4) {
            return "✅ Most features are within normal ranges. The airway structure appears typical based on extracted features.";
        } else {
            return "📋 Feature values show mixed results. Compare with similar cases in dataset for better understanding.";
        }
    }

    private void hideFeatureGuidePopup() {
        featureGuideOverlay.setVisible(false);
        featureGuideOverlay.setManaged(false);
        featureGuideOverlay.getChildren().clear();

        if (mainContent != null && !loadingOverlay.isVisible()) {
            mainContent.setEffect(null);
            mainContent.setDisable(false);
        }
    }

    private void styleStatusBadge() {
        statusLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        statusLabel.setWrapText(true);
        statusLabel.setPadding(new Insets(10, 16, 10, 16));
        updateStatusStyle(statusLabel.getText());
    }

    private void setStatusText(String text) {
        statusLabel.setText(text);
        updateStatusStyle(text);
    }

    private void updateStatusStyle(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        String bg, border, textColor;
        if (lower.contains("error") || lower.contains("failed") || lower.contains("invalid")) {
            bg = "#fee2e2"; border = "#fecaca"; textColor = "#b91c1c";
        } else if (lower.contains("complete") || lower.contains("success")) {
            bg = "#dcfce7"; border = "#bbf7d0"; textColor = "#15803d";
        } else if (lower.contains("processing") || lower.contains("compress") || lower.contains("extract")) {
            bg = "#ede9fe"; border = "#ddd6fe"; textColor = "#6d28d9";
        } else {
            bg = "#e0f2fe"; border = "#bae6fd"; textColor = "#0369a1";
        }
        statusLabel.setStyle(
                "-fx-background-color: " + bg + "; -fx-border-color: " + border + "; " +
                "-fx-border-radius: 100; -fx-background-radius: 100;");
        statusLabel.setTextFill(Color.web(textColor));
    }

    private VBox createImageCard(String title, ImageView imageView, Label placeholder,
                                 double fitWidth, double fitHeight) {
        Label cardTitle = new Label(title);
        cardTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        cardTitle.setTextFill(Color.web("#334155"));

        imageView.setPreserveRatio(false);
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        imageView.setSmooth(true);

        placeholder.setFont(Font.font("System", 13));
        placeholder.setTextFill(Color.web("#94a3b8"));

        StackPane imageArea = new StackPane(imageView, placeholder);
        imageArea.setAlignment(Pos.CENTER);
        imageArea.setMinHeight(fitHeight);
        imageArea.setPrefHeight(fitHeight);
        imageArea.setMinWidth(fitWidth);
        imageArea.setPrefWidth(fitWidth);
        imageArea.setMaxWidth(fitWidth);
        imageArea.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 12;");

        VBox card = new VBox(12, cardTitle, imageArea);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 16;");
        card.setEffect(new DropShadow(8, 0, 2, Color.rgb(0,0,0,0.06)));
        return card;
    }

    private Label createKeyLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#334155"));
        label.setMinWidth(190);
        return label;
    }

    private Label createValueLabel(Label label) {
        label.setFont(Font.font("System", 13));
        label.setTextFill(Color.web("#0f172a"));
        label.setWrapText(true);
        return label;
    }

    private void stylePrimaryButton(Button btn) { styleButton(btn, "#3b82f6", "#2563eb"); }
    private void styleSuccessButton(Button btn) { styleButton(btn, "#10b981", "#059669"); }
    private void styleDangerButton(Button btn)  { styleButton(btn, "#ef4444", "#dc2626"); }

    private void styleButton(Button button, String baseColor, String hoverColor) {
        button.setFont(Font.font("System", FontWeight.BOLD, 13));
        button.setTextFill(Color.WHITE);
        button.setPadding(new Insets(10, 20, 10, 20));
        String base = "-fx-background-color: " + baseColor + "; -fx-background-radius: 100; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + hoverColor + "; -fx-background-radius: 100; -fx-cursor: hand;";
        button.setStyle(base);
        DropShadow btnShadow = new DropShadow(4, 2, 2, Color.rgb(0,0,0,0.15));
        button.setEffect(btnShadow);
        button.setOnMouseEntered(e -> {
            button.setStyle(hover);
            button.setEffect(new DropShadow(6, 3, 3, Color.rgb(0,0,0,0.25)));
        });
        button.setOnMouseExited(e -> {
            button.setStyle(base);
            button.setEffect(btnShadow);
        });
    }

    private void uploadCaseFolder(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select CT Case Folder");
        File folder = chooser.showDialog(stage);
        if (folder != null) {
            selectedCaseFolder = folder;
            latestOutputDir = null;
            latestCompressedAirwayImage = null;
            currentPipelineResult = null;
            originalImageView.setImage(null); finalImageView.setImage(null);
            graphImageView.setImage(null); edgeImageView.setImage(null);
            originalPlaceholder.setVisible(true); finalPlaceholder.setVisible(true);
            edgePlaceholder.setVisible(true); graphPlaceholder.setVisible(true);
            affectedBranchValue.setText(folder.getName());
            narrowingValue.setText("—"); pathTraceValue.setText("—");
            conditionValue.setText("Ready for conversion");
            topSuspiciousNodeValue.setText("—"); topReasonValue.setText("—");
            resetFeatureSection(); resetSimilaritySection(); clearSuspiciousSection();
            extractFeaturesBtn.setDisable(true);
            setStatusText("Case folder selected successfully. Click Process Case.");
        }
    }

    private void processCase(Button processBtn) {
        if (selectedCaseFolder == null) {
            setStatusText("Please upload a case folder first.");
            return;
        }
        Path caseDir = selectedCaseFolder.toPath();
        Path parent = caseDir.getParent();
        if (parent == null) {
            setStatusText("Invalid selected folder.");
            return;
        }
        Path outputRoot = parent.resolve("output");
        Task<ConvertCT.ConversionResult> task = new Task<>() {
            @Override protected ConvertCT.ConversionResult call() throws Exception {
                return ConvertCT.convertCaseFromFolder(caseDir, outputRoot, true, -1000.0, 400.0);
            }
        };
        processBtn.setDisable(true);
        extractFeaturesBtn.setDisable(true);
        setStatusText("Processing case folder...");
        showLoading("Processing CT case folder...");
        task.setOnSucceeded(e -> {
            ConvertCT.ConversionResult result = task.getValue();
            latestOutputDir = result.outputDir;
            affectedBranchValue.setText(selectedCaseFolder.getName());
            narrowingValue.setText(String.valueOf(result.savedCount));
            pathTraceValue.setText(result.outputDir.toString());
            conditionValue.setText("Conversion complete. Starting compression...");
            loadPreviewImages(result.outputDir);
            setStatusText("Conversion completed. Compressing...");
            updateLoadingMessage("Compressing PNG slices...");
            compressCase(processBtn);
        });
        task.setOnFailed(e -> { processBtn.setDisable(false); hideLoading(); setStatusText("Error"); });
        new Thread(task).start();
    }

    private void compressCase(Button processBtn) {
        if (latestOutputDir == null) {
            setStatusText("Process first.");
            processBtn.setDisable(false);
            return;
        }
        Task<PngCaseCompressor.CompressionResult> task = new Task<>() {
            @Override protected PngCaseCompressor.CompressionResult call() throws Exception {
                return PngCaseCompressor.compressAirwayMasks(latestOutputDir);
            }
        };
        setStatusText("Compressing...");
        updateLoadingMessage("Compressing PNG slices...");
        task.setOnSucceeded(e -> {
            processBtn.setDisable(false); hideLoading();
            PngCaseCompressor.CompressionResult res = task.getValue();
            latestCompressedAirwayImage = res.getFinalImage();
            currentPipelineResult = SimilarityFeatureExtractor.runFullPipeline(latestCompressedAirwayImage);
            AirwayImage finalImg = latestCompressedAirwayImage;
            AirwayImage edgeImg = currentPipelineResult.getEdgeImage();
            int[][] mask = currentPipelineResult.getBinaryMask();
            Graph graph = currentPipelineResult.getGraph();
            finalImageView.setImage(finalImg.displayPixels()); finalPlaceholder.setVisible(false);
            edgeImageView.setImage(edgeImg.displayPixels()); edgePlaceholder.setVisible(false);

            PathFind pf = new PathFind(graph);
            AirwayNode start = pf.findStartNode(graph);
            SuspiciousNodeAnalyzer ana = new SuspiciousNodeAnalyzer();
            List<SuspiciousNodeRecord> recs = ana.detectSuspiciousNodes(graph);
            List<SuspiciousNodeRecord> reachable = new ArrayList<>();
            for (SuspiciousNodeRecord r : recs) {
                if (r.getNode() != null && pf.dijkstra(start, r.getNode()) != null)
                    reachable.add(r);
            }
            Map<AirwayNode, Double> scores = new HashMap<>();
            List<AirwayNode> nodes = new ArrayList<>();
            List<SuspiciousItem> items = new ArrayList<>();
            for (SuspiciousNodeRecord r : reachable) {
                scores.put(r.getNode(), r.getSuspicionScore());
                nodes.add(r.getNode());
                items.add(new SuspiciousItem(0, r.getNode(), r.getSuspicionScore(),
                        String.join(", ", r.getReasons())));
            }
            GraphOverlay.OverlayData overlay = GraphOverlay.buildOverlayData(graph,
                    finalImageView.getImage().getWidth(), finalImageView.getImage().getHeight(),
                    mask.length, mask[0].length);
            Image ovImg = createOverlayedImage(finalImageView.getImage(), overlay,
                    new ArrayList<>(), nodes, scores, null, -1, mask.length, mask[0].length);
            graphImageView.setImage(ovImg); graphPlaceholder.setVisible(false);

            currentPathFinder = pf; currentStartNode = start;
            currentBaseImage = finalImageView.getImage(); currentOverlayData = overlay;
            currentMaskRows = mask.length; currentMaskCols = mask[0].length;
            currentCombinedScores = scores; currentSuspiciousNodes = nodes;

            items.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            List<SuspiciousItem> numbered = new ArrayList<>();
            int i = 1;
            for (SuspiciousItem it : items)
                numbered.add(new SuspiciousItem(i++, it.getNode(), it.getScore(), it.getReason()));
            suspiciousListView.getItems().setAll(numbered);
            currentSelectedSuspiciousNumber = -1;
            if (!numbered.isEmpty()) {
                SuspiciousItem top = numbered.get(0);
                topSuspiciousNodeValue.setText("Node " + top.getNodeNumber() +
                        " | score=" + String.format("%.3f", top.getScore()));
                topReasonValue.setText(top.getReason());
            } else {
                topSuspiciousNodeValue.setText("None");
                topReasonValue.setText("No suspicious nodes detected");
            }
            conditionValue.setText("Compression complete. Click Extract Features.");
            setStatusText("Compressed. Suspicious nodes: " + nodes.size());
            extractFeaturesBtn.setDisable(false);
        });
        task.setOnFailed(e -> { processBtn.setDisable(false); hideLoading(); setStatusText("Compression error"); });
        new Thread(task).start();
    }

    private void extractFeatures() {
        if (currentPipelineResult == null) {
            setStatusText("Process and compress first.");
            return;
        }

        hideFeatureGuidePopup();
        extractFeaturesBtn.setDisable(true);
        setStatusText("Extracting features...");
        showLoading("Extracting structural airway features...");

        Task<FeatureTaskResult> task = new Task<>() {
            @Override protected FeatureTaskResult call() throws Exception {
                updateMessage("Extracting structural features...");
                ComparisonResult cr = null;
                String err = null;
                try {
                    Path dataPath = resolveDatasetFeaturesPath();
                    cr = SimilarityFeatureExtractor.compareWithDataset(
                            currentPipelineResult.getFeatures(), dataPath, 3);
                } catch (Exception ex) {
                    err = ex.getMessage();
                }
                return new FeatureTaskResult(currentPipelineResult, cr, err);
            }
        };

        task.messageProperty().addListener((obs, old, val) -> {
            if (val != null) setStatusText(val.replace("Status: ", ""));
        });

        task.setOnSucceeded(e -> {
            extractFeaturesBtn.setDisable(false);
            hideLoading();

            FeatureTaskResult r = task.getValue();
            SimilarityFeatures features = r.getPipelineResult().getFeatures();

            updateFeatureSection(features);
            showFeatureGuidePopup(features);

            if (r.getComparisonResult() != null) {
                updateSimilaritySection(r.getComparisonResult());
                conditionValue.setText("Feature extraction and similarity complete.");
                setStatusText("Features and similarity extracted successfully.");
            } else {
                resetSimilaritySection();
                similarityExplanationLabel.setText("Features extracted, similarity failed. " +
                        (r.getSimilarityError() == null ? "" : "Reason: " + r.getSimilarityError()));
                conditionValue.setText("Feature extraction complete. Similarity not completed.");
                setStatusText("Features extracted, similarity failed.");
            }
        });

        task.setOnFailed(e -> {
            extractFeaturesBtn.setDisable(false);
            hideLoading();
            setStatusText("Feature extraction error");
        });

        new Thread(task).start();
    }

    private List<NeighborMatch> filterMatches(List<NeighborMatch> allMatches, double threshold) {
        List<NeighborMatch> filtered = new ArrayList<>();

        for (NeighborMatch match : allMatches) {
            double sim = match.getSimilarityPercentage();

            if (sim > threshold) {
                continue;
            }

            filtered.add(match);
        }

        return filtered;
    }

    private void updateFeatureSection(SimilarityFeatures f) {
        branchCountValue.setText(
                f.getBranchCount() + " branches - " + interpretBranchCount(f.getBranchCount()));

        branchPointCountValue.setText(
                f.getBranchPointCount() + " branch-point nodes - " +
                interpretBranchPointCount(f.getBranchPointCount()));

        averageTaperingValue.setText(
                formatDecimal(f.getAverageTapering()) + " - " +
                interpretTapering(f.getAverageTapering()));

        averageTortuosityValue.setText(
                formatDecimal(f.getAverageTortuosity()) + " - " +
                interpretTortuosity(f.getAverageTortuosity()));

        abruptEndingRatioValue.setText(
                formatDecimal(f.getAbruptEndingRatio()) + " - " +
                interpretAbruptEnding(f.getAbruptEndingRatio()));

        extractedFeatureExplanationLabel.setText(
                "The system extracted structural airway features from the final compressed image and graph.\n\n" +
                "Branch Count means the total number of airway branches and endpoints detected in the tree.\n" +
                "Branch Point Count means the number of places where one airway splits into two.\n" +
                "Average Tapering means how fast the airways become narrower.\n" +
                "Average Tortuosity means how curvy or winding the airways are.\n" +
                "Abrupt-Ending Ratio means how many airways appear to cut off suddenly.\n\n" +
                "Simple project ranges:\n" +
                "Branch Count normal range: 100 - 200. Suspicious if less than 100 or greater than 200.\n" +
                "Branch Point Count normal range: 60 - 120. Suspicious if less than 60 or greater than 120.\n" +
                "Tapering is normal below 0.03 and suspicious above 0.05.\n" +
                "Tortuosity is normal below 1.2 and suspicious above 1.3.\n" +
                "Abrupt-Ending is normal below 0.2 and suspicious above 0.3.\n\n" +
                "These explanations are only project guidance, not a medical diagnosis."
        );
    }

    private String interpretBranchCount(int value) {
        if (value < 50) {
            return "❌ Very Low: very few airways detected, possible severe disease or poor scan.";
        } else if (value < 100) {
            return "⚠ Low: fewer airways than normal, possible obstruction.";
        } else if (value <= 150) {
            return "✅ Normal: typical healthy airway tree.";
        } else if (value <= 200) {
            return "✅ Normal: healthy, well-developed airway tree.";
        } else {
            return "⚠ High: extra branches detected, possible false positives or noisy scan.";
        }
    }

    private String interpretBranchPointCount(int value) {
        if (value < 30) {
            return "❌ Very Low: very few splits detected, possible severe disease.";
        } else if (value < 60) {
            return "⚠ Low: fewer splits than normal, possible obstruction.";
        } else if (value <= 90) {
            return "✅ Normal: typical number of airway splits.";
        } else if (value <= 120) {
            return "✅ Normal: healthy branching pattern.";
        } else {
            return "⚠ High: too many splits, possible noise or over-detection.";
        }
    }

    private String interpretTapering(double value) {
        if (value <= 0.03) {
            return "✅ Normal tapering: airways narrow normally.";
        } else if (value <= 0.05) {
            return "⚠ Slightly High tapering: airways may be narrowing faster than expected.";
        } else {
            return "⚠ High tapering: airways narrow too quickly, possible disease.";
        }
    }

    private String interpretTortuosity(double value) {
        if (value <= 1.2) {
            return "✅ Normal tortuosity: airways are mostly straight.";
        } else if (value <= 1.3) {
            return "⚠ Slightly High tortuosity: airways are more winding than expected.";
        } else {
            return "⚠ High tortuosity: airways are too winding, possible scarring.";
        }
    }

    private String interpretAbruptEnding(double value) {
        if (value <= 0.2) {
            return "✅ Normal abrupt-ending ratio: few airways end suddenly.";
        } else if (value <= 0.3) {
            return "⚠ Slightly High abrupt endings: some airways may be cutting off.";
        } else {
            return "⚠ High abrupt endings: many airways end suddenly, possible airway blockage.";
        }
    }

    private void updateSimilaritySection(ComparisonResult originalResult) {
        double THRESHOLD = 95.0;

        List<NeighborMatch> allMatches = originalResult.getNearestMatches();
        List<NeighborMatch> filteredMatches = filterMatches(allMatches, THRESHOLD);

        NeighborMatch bestMatch;
        List<NeighborMatch> topKMatches;
        int kUsed = originalResult.getKUsed();

        if (filteredMatches.isEmpty()) {
            knnValue.setText(String.valueOf(kUsed));
            bestMatchCaseValue.setText("All matches are the same patient");
            bestMatchPercentageValue.setText("—");
            nearestCasesValue.setText("No different cases found");
            similarityExplanationLabel.setText(
                "Uploaded case is identical or extremely similar to all " + kUsed +
                " nearest cases. This suggests the patient is already in the dataset."
            );
            return;
        }

        topKMatches = new ArrayList<>();
        for (int i = 0; i < Math.min(kUsed, filteredMatches.size()); i++) {
            topKMatches.add(filteredMatches.get(i));
        }

        bestMatch = topKMatches.get(0);

        knnValue.setText(String.valueOf(kUsed));
        bestMatchCaseValue.setText("Case " + bestMatch.getDatasetCase().getCaseId());
        bestMatchPercentageValue.setText(String.format("%.2f%%", bestMatch.getSimilarityPercentage()));

        StringBuilder nearestBuilder = new StringBuilder();
        for (int i = 0; i < topKMatches.size(); i++) {
            NeighborMatch match = topKMatches.get(i);
            if (i > 0) nearestBuilder.append(" | ");
            nearestBuilder.append("Case ")
                          .append(match.getDatasetCase().getCaseId())
                          .append(" (")
                          .append(String.format("%.2f%%", match.getSimilarityPercentage()))
                          .append(")");
        }
        nearestCasesValue.setText(nearestBuilder.toString());

        int skippedCount = allMatches.size() - filteredMatches.size();
        String explanation;

        if (skippedCount > 0) {
            explanation = String.format(
                "Skipped %d identical/duplicate case(s) (similarity > %.0f%%). " +
                "Most similar different case: Case %s with %.2f%% similarity. " +
                "K=%d nearest different cases: %s",
                skippedCount, THRESHOLD,
                bestMatch.getDatasetCase().getCaseId(),
                bestMatch.getSimilarityPercentage(),
                kUsed, nearestBuilder.toString()
            );
        } else {
            explanation = String.format(
                "Through KNN comparison, the uploaded case is most similar to Case %s " +
                "with %.2f%% similarity. K=%d nearest cases: %s",
                bestMatch.getDatasetCase().getCaseId(),
                bestMatch.getSimilarityPercentage(),
                kUsed, nearestBuilder.toString()
            );
        }

        similarityExplanationLabel.setText(explanation);
    }

    private Path resolveDatasetFeaturesPath() throws IOException {
        Path src = Paths.get("src", "dataset_features.txt");
        if (Files.exists(src)) return src;
        Path root = Paths.get("dataset_features.txt");
        if (Files.exists(root)) return root;
        throw new IOException("dataset_features.txt not found.");
    }

    private void loadPreviewImages(Path dir) {
        Path ctDir = dir.resolve("ct_coronal_png");
        Image img = loadFirstPng(ctDir);
        originalImageView.setImage(img);
        originalPlaceholder.setVisible(img == null);
    }

    private Image loadFirstPng(Path folder) {
        if (folder == null || !Files.isDirectory(folder)) return null;
        try (Stream<Path> s = Files.list(folder)) {
            Optional<Path> first = s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .findFirst();
            if (first.isPresent()) return new Image(first.get().toUri().toString());
        } catch (IOException ignored) {}
        return null;
    }

    private void resetFeatureSection() {
        branchCountValue.setText("—"); branchPointCountValue.setText("—");
        averageTaperingValue.setText("—"); averageTortuosityValue.setText("—");
        abruptEndingRatioValue.setText("—");
        extractedFeatureExplanationLabel.setText("No structural features extracted yet.");
    }

    private void resetSimilaritySection() {
        knnValue.setText("—"); bestMatchCaseValue.setText("—");
        bestMatchPercentageValue.setText("—"); nearestCasesValue.setText("—");
        similarityExplanationLabel.setText("No similarity comparison performed yet.");
    }

    private void clearSuspiciousSection() {
        topSuspiciousNodeValue.setText("—");
        topReasonValue.setText("—");
        suspiciousListView.getItems().clear();
        currentPathFinder = null; currentStartNode = null; currentBaseImage = null;
        currentOverlayData = null; currentMaskRows = 0; currentMaskCols = 0;
        currentCombinedScores = new HashMap<>(); currentSuspiciousNodes = new ArrayList<>();
    }

    private void clearAll() {
        selectedCaseFolder = null; latestOutputDir = null; latestCompressedAirwayImage = null;
        currentPipelineResult = null;
        originalImageView.setImage(null); finalImageView.setImage(null);
        graphImageView.setImage(null); edgeImageView.setImage(null);
        originalPlaceholder.setVisible(true); finalPlaceholder.setVisible(true);
        edgePlaceholder.setVisible(true); graphPlaceholder.setVisible(true);
        affectedBranchValue.setText("—"); narrowingValue.setText("—");
        pathTraceValue.setText("—"); conditionValue.setText("—");
        resetFeatureSection(); resetSimilaritySection(); clearSuspiciousSection();
        extractFeaturesBtn.setDisable(true);
        hideFeatureGuidePopup();
        hideLoading();
        setStatusText("Cleared. Waiting for case folder upload...");
    }

    private Image createOverlayedImage(
            Image baseImage, GraphOverlay.OverlayData overlayData,
            List<GraphOverlay.OverlayLine> pathLines,
            List<AirwayNode> suspiciousNodes, Map<AirwayNode, Double> combinedScores,
            AirwayNode selectedSuspiciousNode, int selectedSuspiciousNumber,
            int maskRows, int maskCols) {
        double w = baseImage.getWidth(), h = baseImage.getHeight();
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(w, h);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(baseImage, 0, 0, w, h);

        gc.setStroke(Color.RED); gc.setLineWidth(2.8);
        for (GraphOverlay.OverlayLine l : overlayData.getLines())
            gc.strokeLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());

        for (GraphOverlay.OverlayNode n : overlayData.getNodes()) {
            switch (n.getType()) {
                case START: gc.setFill(Color.BLUE); break;
                case END:   gc.setFill(Color.LIMEGREEN); break;
                case BRANCH: continue;
                default:    gc.setFill(Color.WHITE);
            }
            gc.fillOval(n.getX() - 4.5, n.getY() - 4.5, 9, 9);
        }

        double sx = w / maskCols, sy = h / maskRows;
        gc.setFill(Color.web("#27D3F5"));
        for (AirwayNode node : suspiciousNodes) {
            double score = combinedScores.getOrDefault(node, 0.0);
            double x = node.getCol() * sx, y = node.getRow() * sy;
            double d = 5.0 + score * 5.0;
            gc.fillOval(x - d / 2, y - d / 2, d, d);
        }

        gc.setStroke(Color.YELLOW); gc.setLineWidth(6);
        for (GraphOverlay.OverlayLine l : pathLines)
            gc.strokeLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());

        if (selectedSuspiciousNode != null) {
            double x = selectedSuspiciousNode.getCol() * sx, y = selectedSuspiciousNode.getRow() * sy;
            gc.setFill(Color.web("#2310AD")); gc.fillOval(x - 8, y - 8, 16, 16);
            if (selectedSuspiciousNumber > 0) {
                gc.setFill(Color.WHITE); gc.fillRoundRect(x + 10, y - 22, 44, 24, 10, 10);
                gc.setStroke(Color.web("#2310AD")); gc.setLineWidth(1.5);
                gc.strokeRoundRect(x + 10, y - 22, 44, 24, 10, 10);
                gc.setFill(Color.web("#2310AD"));
                gc.setFont(Font.font("System", FontWeight.BOLD, 13));
                gc.fillText(String.valueOf(selectedSuspiciousNumber), x + 27, y - 6);
            }
        }
        WritableImage result = new WritableImage((int) Math.ceil(w), (int) Math.ceil(h));
        return canvas.snapshot(new SnapshotParameters(), result);
    }

    private void showPathToSuspiciousNode(SuspiciousItem item) {
        if (item == null) return;
        AirwayNode target = item.getNode();
        currentSelectedSuspiciousNumber = item.getNodeNumber();
        if (currentPathFinder == null || currentStartNode == null || currentBaseImage == null) return;
        List<AirwayNode> path = currentPathFinder.dijkstra(currentStartNode, target);
        if (path == null || path.isEmpty()) {
            setStatusText("Unreachable node.");
            return;
        }
        List<GraphOverlay.OverlayLine> lines = buildPathLines(path,
                currentBaseImage.getWidth(), currentBaseImage.getHeight(),
                currentMaskRows, currentMaskCols);
        Image newImg = createOverlayedImage(currentBaseImage, currentOverlayData, lines,
                currentSuspiciousNodes, currentCombinedScores,
                target, currentSelectedSuspiciousNumber,
                currentMaskRows, currentMaskCols);
        graphImageView.setImage(newImg);
        setStatusText("Showing path to Node " + item.getNodeNumber());
    }

    private List<GraphOverlay.OverlayLine> buildPathLines(List<AirwayNode> path,
            double imgW, double imgH, int maskR, int maskC) {
        List<GraphOverlay.OverlayLine> lines = new ArrayList<>();
        if (path == null || path.size() < 2) return lines;
        double sx = imgW / maskC, sy = imgH / maskR;
        for (int i = 0; i < path.size() - 1; i++) {
            AirwayNode a = path.get(i), b = path.get(i + 1);
            lines.add(new GraphOverlay.OverlayLine(a.getCol() * sx, a.getRow() * sy,
                    b.getCol() * sx, b.getRow() * sy));
        }
        return lines;
    }

    private String formatDecimal(double v) {
        return String.format("%.6f", v);
    }
}