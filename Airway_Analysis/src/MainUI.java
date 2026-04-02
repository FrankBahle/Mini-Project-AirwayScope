import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainUI {

    public Scene createScene(Stage stage) {

        // ── TOP HEADER BAR ──────────────────────────────────────────
        Label titleLabel = new Label("Airway Analysis System");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("Respiratory Decision-Support Tool");
        subtitleLabel.setFont(Font.font("Segoe UI", 13));
        subtitleLabel.setTextFill(Color.LIGHTBLUE);

        VBox titleBox = new VBox(2, titleLabel, subtitleLabel);

        Button uploadBtn = new Button("⬆  Upload Image");
        Button processBtn = new Button("⚙  Process");
        Button clearBtn   = new Button("✕  Clear");

        styleButton(uploadBtn, "#1a73e8");
        styleButton(processBtn, "#0f9d58");
        styleButton(clearBtn, "#d93025");

        HBox actionButtons = new HBox(10, uploadBtn, processBtn, clearBtn);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        BorderPane header = new BorderPane();
        header.setLeft(titleBox);
        header.setRight(actionButtons);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #1a1f2e;");
        BorderPane.setAlignment(titleBox, Pos.CENTER_LEFT);
        BorderPane.setAlignment(actionButtons, Pos.CENTER_RIGHT);

        // ── IMAGE PANELS ────────────────────────────────────────────
        VBox originalPanel  = createImagePanel("Original Image",  "#1e2a3a");
        VBox processedPanel = createImagePanel("Processed Image", "#1e2a3a");
        VBox graphPanel     = createImagePanel("Graph Overlay",   "#1e2a3a");

        HBox imagePanels = new HBox(10, originalPanel, processedPanel, graphPanel);
        imagePanels.setPadding(new Insets(10, 15, 5, 15));
        HBox.setHgrow(originalPanel,  Priority.ALWAYS);
        HBox.setHgrow(processedPanel, Priority.ALWAYS);
        HBox.setHgrow(graphPanel,     Priority.ALWAYS);

        // ── RESULTS PANEL ───────────────────────────────────────────
        Label resultsTitle = new Label("Analysis Results");
        resultsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        resultsTitle.setTextFill(Color.WHITE);

        HBox affectedBranch = makeResultLabel("Affected Branch:", "—");
        HBox narrowing      = makeResultLabel("Narrowing Level:", "—");
        HBox pathTrace      = makeResultLabel("Path Trace:", "—");
        HBox possibleDisease = makeResultLabel("Possible Condition:", "—");

        VBox resultItems = new VBox(10,
                new Separator(),
                affectedBranch,
                narrowing,
                pathTrace,
                possibleDisease,
                new Separator()
        );
        resultItems.setPadding(new Insets(10, 0, 0, 0));

        Label statusLabel = new Label("Status: Awaiting image upload...");
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.LIGHTGRAY);

        VBox resultsBox = new VBox(10, resultsTitle, resultItems, statusLabel);
        resultsBox.setPadding(new Insets(15));
        resultsBox.setStyle("-fx-background-color: #1e2a3a; -fx-background-radius: 8;");

        ScrollPane resultsScroll = new ScrollPane(resultsBox);
        resultsScroll.setFitToWidth(true);
        resultsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        HBox bottomSection = new HBox(10, resultsScroll);
        bottomSection.setPadding(new Insets(5, 15, 15, 15));
        HBox.setHgrow(resultsScroll, Priority.ALWAYS);

        // ── BUTTON ACTIONS ──────────────────────────────────────────
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Airway Image");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
            );
            fileChooser.showOpenDialog(stage);
            statusLabel.setText("Status: Image loaded. Ready to process.");
        });

        clearBtn.setOnAction(e -> {
            statusLabel.setText("Status: Cleared. Awaiting image upload...");
        });

        // ── ROOT LAYOUT ─────────────────────────────────────────────
        VBox root = new VBox(0, header, imagePanels, bottomSection);
        VBox.setVgrow(imagePanels, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #12171f;");

        return new Scene(root, 1200, 750);
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    private VBox createImagePanel(String title, String bgColor) {
        Label label = new Label(title);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        label.setTextFill(Color.LIGHTGRAY);

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(320);
        imageView.setFitHeight(300);

        Label placeholder = new Label("No image loaded");
        placeholder.setTextFill(Color.GRAY);
        placeholder.setFont(Font.font("Segoe UI", 12));

        StackPane imageArea = new StackPane(imageView, placeholder);
        imageArea.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6;");
        imageArea.setPrefHeight(300);
        VBox.setVgrow(imageArea, Priority.ALWAYS);

        VBox panel = new VBox(8, label, imageArea);
        panel.setPadding(new Insets(12));
        panel.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");
        VBox.setVgrow(panel, Priority.ALWAYS);

        return panel;
    }

    private HBox makeResultLabel(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        keyLabel.setTextFill(Color.LIGHTBLUE);
        keyLabel.setMinWidth(160);

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", 12));
        valueLabel.setTextFill(Color.WHITE);

        return new HBox(10, keyLabel, valueLabel);
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 8 16 8 16;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
    }
}