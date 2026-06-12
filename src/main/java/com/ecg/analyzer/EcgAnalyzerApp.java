package com.ecg.analyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX Application class for ECG Analyzer.
 * Entry point for the application.
 */
public class EcgAnalyzerApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(EcgAnalyzerApp.class);
    private static final String WINDOW_TITLE = "ECG Anomaly Analyzer";
    private static final int WINDOW_WIDTH = 1400;
    private static final int WINDOW_HEIGHT = 900;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting ECG Analyzer Application");
            
            // Load FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/MainView.fxml")
            );
            Parent root = loader.load();
            
            // Create scene
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Configure primary stage
            primaryStage.setTitle(WINDOW_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(700);
            
            // Show window
            primaryStage.show();
            
            logger.info("Application started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorAndExit(e);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Application shutting down");
    }
    
    /**
     * Shows error dialog and exits application.
     */
    private void showErrorAndExit(Exception e) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Application Error");
        alert.setHeaderText("Failed to start ECG Analyzer");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
        System.exit(1);
    }
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        logger.info("Launching ECG Analyzer...");
        launch(args);
    }
}
