package com.iwfc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX Application entry point for the IWFC Management System.
 *
 * <p>Loads {@code MainView.fxml}, attaches styling and presents the primary window.</p>
 */
public class MainApp extends Application {

    private static final String TITLE   = "IWFC Management System";
    private static final double WIDTH   = 900;
    private static final double HEIGHT  = 600;

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(
                        getClass().getResource("/com/iwfc/MainView.fxml"),
                        "Cannot find MainView.fxml on classpath"
                )
        );

        Scene scene = new Scene(root, WIDTH, HEIGHT);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/com/iwfc/styles.css"),
                        "Cannot find styles.css on classpath"
                ).toExternalForm()
        );

        primaryStage.setTitle(TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(WIDTH);
        primaryStage.setMinHeight(HEIGHT);
        primaryStage.show();
    }

    /** Called by {@link Main} – forwards args to the JavaFX launcher. */
    public static void main(String[] args) {
        launch(args);
    }
}
