import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private final ImageView processedImageView = new ImageView();
    private final ImageView graphImageView = new ImageView();
    private final ImageView finalImageView = new ImageView();

    private final Label originalPlaceholder = new Label("No case folder selected");
    private final Label processedPlaceholder = new Label("No processed image yet");
    private final Label graphPlaceholder = new Label("No graph overlay yet");
    private final Label finalPlaceholder = new Label("No final compressed image yet");

    private final Label affectedBranchValue = new Label("—");
    private final Label narrowingValue = new Label("—");
    private final Label pathTraceValue = new Label("—");
    private final Label conditionValue = new Label("—");
    private final Label statusLabel = new Label("Status: Waiting for case folder upload...");

    private File selectedCaseFolder;
    private Path latestOutputDir;

    public Scene createScene(Stage stage) {

        Label titleLabel = new Label("Airway Analysis System");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#1f2937"));

        Label subtitleLabel = new Label("Convert CT case folders to PNG slices, then compress them into one airway image");
        subtitleLabel.setFont(Font.font("Segoe UI", 13));
        subtitleLabel.setTextFill(Color.web("#6b7280"));

        VBox titleBox = new VBox(4, titleLabel, subtitleLabel);

        Button uploadBtn = new Button("Upload Case Folder");
        Button processBtn = new Button("Process Case");
        Button compressBtn = new Button("Compress PNGs");
        Button clearBtn = new Button("Clear");

        stylePrimaryButton(uploadBtn);
        styleSuccessButton(processBtn);
        styleSuccessButton(compressBtn);
        styleDangerButton(clearBtn);

        HBox buttonBox = new HBox(10, uploadBtn, processBtn, compressBtn, clearBtn);
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
        VBox processedCard = createImageCard("Airway Mask Preview", processedImageView, processedPlaceholder);
        VBox graphCard = createImageCard("Overlay Preview", graphImageView, graphPlaceholder);
        VBox finalCard = createImageCard("Final Compressed Image", finalImageView, finalPlaceholder);

        HBox imageSection = new HBox(15, originalCard, processedCard, graphCard, finalCard);
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

        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#4b5563"));

        VBox resultsCard = new VBox(15,
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

        HBox bottomSection = new HBox(resultsCard);
        bottomSection.setPadding(new Insets(0, 20, 20, 20));
        HBox.setHgrow(resultsCard, Priority.ALWAYS);

        VBox content = new VBox(0, header, imageScrollPane, bottomSection);
        VBox.setVgrow(imageScrollPane, Priority.ALWAYS);
        content.setStyle("-fx-background-color: #f7f9fc;");

        BorderPane root = new BorderPane(content);

        uploadBtn.setOnAction(e -> uploadCaseFolder(stage));
        processBtn.setOnAction(e -> processCase(processBtn));
        compressBtn.setOnAction(e -> compressCase(compressBtn));
        clearBtn.setOnAction(e -> clearAll());

        return new Scene(root, 1450, 820);
    }

    private VBox createImageCard(String title, ImageView imageView, Label placeholder) {
        Label cardTitle = new Label(title);
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        cardTitle.setTextFill(Color.web("#1f2937"));

        imageView.setPreserveRatio(true);
        imageView.setFitWidth(320);
        imageView.setFitHeight(250);
        imageView.setSmooth(true);

        placeholder.setFont(Font.font("Segoe UI", 13));
        placeholder.setTextFill(Color.web("#94a3b8"));

        StackPane imageArea = new StackPane(imageView, placeholder);
        imageArea.setAlignment(Pos.CENTER);
        imageArea.setMinHeight(280);
        imageArea.setPrefHeight(280);
        imageArea.setMinWidth(330);
        imageArea.setPrefWidth(330);
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

    private Label createKeyLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#334155"));
        label.setMinWidth(160);
        return label;
    }

    private Label createValueLabel(Label label) {
        label.setFont(Font.font("Segoe UI", 13));
        label.setTextFill(Color.web("#111827"));
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

            originalImageView.setImage(null);
            processedImageView.setImage(null);
            graphImageView.setImage(null);
            finalImageView.setImage(null);

            originalPlaceholder.setVisible(true);
            processedPlaceholder.setVisible(true);
            graphPlaceholder.setVisible(true);
            finalPlaceholder.setVisible(true);

            affectedBranchValue.setText(folder.getName());
            narrowingValue.setText("—");
            pathTraceValue.setText("—");
            conditionValue.setText("Ready for conversion");

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
        statusLabel.setText("Status: Processing case folder...");

        task.setOnSucceeded(e -> {
            processBtn.setDisable(false);

            ConvertCT.ConversionResult result = task.getValue();
            latestOutputDir = result.outputDir;

            affectedBranchValue.setText(selectedCaseFolder.getName());
            narrowingValue.setText(String.valueOf(result.savedCount));
            pathTraceValue.setText(result.outputDir.toString());
            conditionValue.setText("Conversion complete. You can now compress the PNG slices.");

            loadPreviewImages(result.outputDir);
            statusLabel.setText("Status: Conversion completed successfully.");
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

    private void compressCase(Button compressBtn) {
        if (latestOutputDir == null) {
            statusLabel.setText("Status: Please process the case first before compressing.");
            return;
        }

        Task<PngCaseCompressor.CompressionResult> task = new Task<>() {
            @Override
            protected PngCaseCompressor.CompressionResult call() throws Exception {
                return PngCaseCompressor.compressAirwayMasks(latestOutputDir);
            }
        };

        compressBtn.setDisable(true);
        statusLabel.setText("Status: Compressing PNG slices into one final image...");

        task.setOnSucceeded(e -> {
            compressBtn.setDisable(false);

            PngCaseCompressor.CompressionResult result = task.getValue();

            finalImageView.setImage(result.getFinalImage().displayPixels());
            finalPlaceholder.setVisible(false);

            conditionValue.setText("Compression complete. Final airway image created.");
            statusLabel.setText("Status: Final compressed airway image created successfully.");
        });

        task.setOnFailed(e -> {
            compressBtn.setDisable(false);
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

    private void loadPreviewImages(Path outputDir) {
        Path ctDir = outputDir.resolve("ct_coronal_png");
        Path airwayDir = outputDir.resolve("airway_coronal_png");
        Path overlayDir = outputDir.resolve("overlay_coronal_png");

        Image ctImage = loadFirstPng(ctDir);
        Image airwayImage = loadFirstPng(airwayDir);
        Image overlayImage = loadFirstPng(overlayDir);

        originalImageView.setImage(ctImage);
        processedImageView.setImage(airwayImage);
        graphImageView.setImage(overlayImage);

        originalPlaceholder.setVisible(ctImage == null);
        processedPlaceholder.setVisible(airwayImage == null);
        graphPlaceholder.setVisible(overlayImage == null);
    }

    private Image loadFirstPng(Path folder) {
        if (folder == null || !Files.exists(folder) || !Files.isDirectory(folder)) {
            return null;
        }

        try (Stream<Path> stream = Files.list(folder)) {
            Optional<Path> first = stream
                    .filter(p -> Files.isRegularFile(p))
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

    private void clearAll() {
        selectedCaseFolder = null;
        latestOutputDir = null;

        originalImageView.setImage(null);
        processedImageView.setImage(null);
        graphImageView.setImage(null);
        finalImageView.setImage(null);

        originalPlaceholder.setVisible(true);
        processedPlaceholder.setVisible(true);
        graphPlaceholder.setVisible(true);
        finalPlaceholder.setVisible(true);

        affectedBranchValue.setText("—");
        narrowingValue.setText("—");
        pathTraceValue.setText("—");
        conditionValue.setText("—");

        statusLabel.setText("Status: Cleared. Waiting for case folder upload...");
    }
}
