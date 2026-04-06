import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainUI ui = new MainUI();
        primaryStage.setTitle("Airway Analysis System");
        primaryStage.setScene(ui.createScene(primaryStage));
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
