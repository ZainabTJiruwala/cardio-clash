package com.ecg.analyzer.controller;

import com.ecg.analyzer.model.Anomaly;
import com.ecg.analyzer.model.EcgDataPoint;
import com.ecg.analyzer.model.EcgSignal;
import com.ecg.analyzer.repository.CsvEcgDataRepository;
import com.ecg.analyzer.service.AnalysisService;
import com.ecg.analyzer.service.detector.MLBasedDetector;
import com.ecg.analyzer.service.detector.RuleBasedDetector;
import com.ecg.analyzer.service.export.PdfReportExporter;
import com.ecg.analyzer.util.ChartExporter;
import com.ecg.analyzer.util.UserPreferences;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * JavaFX Controller for the ECG Analyzer main window.
 * Handles user interactions and coordinates between UI and business logic.
 */
public class EcgAnalyzerController {
    
    private static final Logger logger = LoggerFactory.getLogger(EcgAnalyzerController.class);
    
    // FXML injected components - Control Panel
    @FXML private Button loadFileButton;
    @FXML private Button runAnalysisButton;
    @FXML private Button clearResultsButton;
    @FXML private Button exportPdfButton;
    @FXML private Button exportChartButton;
    @FXML private Button themeToggleButton;
    @FXML private Label fileInfoLabel;
    @FXML private RadioButton ruleBasedRadio;
    @FXML private RadioButton mlBasedRadio;
    @FXML private CheckBox detectMiCheckbox;
    @FXML private CheckBox detectMurmurCheckbox;
    @FXML private CheckBox bandpassFilterCheckbox;
    @FXML private CheckBox baselineRemovalCheckbox;
    @FXML private CheckBox noiseReductionCheckbox;
    
    // FXML injected components - Visualization
    @FXML private LineChart<Number, Number> ecgChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    
    // FXML injected components - Metrics Dashboard
    @FXML private Label heartRateLabel;
    @FXML private Label heartRateStatus;
    @FXML private Label rrIntervalLabel;
    @FXML private Label rPeaksCountLabel;
    @FXML private Label processingTimeLabel;
    
    // FXML injected components - Results Table
    @FXML private TableView<AnomalyRow> anomalyTable;
    @FXML private TableColumn<AnomalyRow, String> timestampColumn;
    @FXML private TableColumn<AnomalyRow, String> typeColumn;
    @FXML private TableColumn<AnomalyRow, String> severityColumn;
    @FXML private TableColumn<AnomalyRow, String> confidenceColumn;
    @FXML private TableColumn<AnomalyRow, String> detailsColumn;
    
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    
    // Business logic
    private AnalysisService analysisService;
    private EcgSignal currentSignal;
    private AnalysisService.AnalysisResult currentResult;
    private UserPreferences userPreferences;
    private PdfReportExporter pdfExporter;
    private String currentFilePath;
    
    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing ECG Analyzer Controller");
        
        // Initialize services
        analysisService = new AnalysisService(
            new CsvEcgDataRepository(),
            new RuleBasedDetector()
        );
        userPreferences = new UserPreferences();
        pdfExporter = new PdfReportExporter();
        
        // Configure chart
        setupChart();
        
        // Configure table
        setupTable();
        
        // Configure buttons
        runAnalysisButton.setDisable(true);
        clearResultsButton.setDisable(true);
        exportPdfButton.setDisable(true);
        exportChartButton.setDisable(true);
        
        // Set up detector change listener
        ruleBasedRadio.setOnAction(e -> switchToRuleBasedDetector());
        mlBasedRadio.setOnAction(e -> switchToMLDetector());
        
        // Set initial status
        updateStatus("Ready. Load an ECG file to begin.");
        progressBar.setVisible(false);
        
        // Load and apply theme preference after scene is ready
        Platform.runLater(() -> {
            String theme = userPreferences.getTheme();
            applyTheme(theme);
            logger.info("Applied initial theme: {}", theme);
        });
        
        logger.info("Controller initialized successfully");
    }
    
    /**
     * Sets up the ECG chart configuration.
     */
    private void setupChart() {
        xAxis.setLabel("Time (seconds)");
        yAxis.setLabel("Amplitude (mV)");
        ecgChart.setTitle("ECG Signal");
        ecgChart.setCreateSymbols(false); // Don't show data point markers
        ecgChart.setLegendVisible(true);
    }
    
    /**
     * Sets up the anomaly results table.
     */
    private void setupTable() {
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        confidenceColumn.setCellValueFactory(new PropertyValueFactory<>("confidence"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        
        // Allow row selection to jump to anomaly location
        anomalyTable.setRowFactory(tv -> {
            TableRow<AnomalyRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    AnomalyRow anomalyRow = row.getItem();
                    highlightAnomalyInChart(anomalyRow);
                }
            });
            return row;
        });
    }
    
    /**
     * Handles Load File button click.
     */
    @FXML
    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open ECG Data File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(loadFileButton.getScene().getWindow());
        if (file != null) {
            loadEcgFile(file.toPath());
        }
    }
    
    /**
     * Loads ECG file in background thread.
     */
    private void loadEcgFile(Path filePath) {
        Task<EcgSignal> loadTask = new Task<>() {
            @Override
            protected EcgSignal call() throws Exception {
                updateMessage("Loading file...");
                return analysisService.loadEcgData(filePath);
            }
        };
        
        loadTask.setOnSucceeded(event -> {
            currentSignal = loadTask.getValue();
            currentFilePath = filePath.toString();
            displaySignal(currentSignal);
            runAnalysisButton.setDisable(false);
            exportChartButton.setDisable(false);
            fileInfoLabel.setText(String.format("%s | %d points | %.2fs | %d Hz",
                currentSignal.recordId(),
                currentSignal.size(),
                currentSignal.duration(),
                currentSignal.samplingRate()
            ));
            updateStatus("File loaded successfully. Click 'Run Analysis' to detect anomalies.");
            progressBar.setVisible(false);
        });
        
        loadTask.setOnFailed(event -> {
            showError("Failed to load file", loadTask.getException().getMessage());
            progressBar.setVisible(false);
        });
        
        progressBar.setVisible(true);
        progressBar.progressProperty().bind(loadTask.progressProperty());
        updateStatus("Loading file: " + filePath.getFileName());
        
        new Thread(loadTask).start();
    }
    
    /**
     * Displays the ECG signal in the chart.
     */
    private void displaySignal(EcgSignal signal) {
        ecgChart.getData().clear();
        
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("ECG Signal");
        
        // Add data points (subsample if too many)
        List<EcgDataPoint> dataPoints = signal.dataPoints();
        int step = Math.max(1, dataPoints.size() / 2000); // Max 2000 points for performance
        
        for (int i = 0; i < dataPoints.size(); i += step) {
            EcgDataPoint point = dataPoints.get(i);
            series.getData().add(new XYChart.Data<>(point.time(), point.amplitude()));
        }
        
        ecgChart.getData().add(series);
        logger.info("Displayed {} data points in chart", series.getData().size());
    }
    
    /**
     * Handles Run Analysis button click.
     */
    @FXML
    private void handleRunAnalysis() {
        if (currentSignal == null) return;
        
        Task<AnalysisService.AnalysisResult> analysisTask = new Task<>() {
            @Override
            protected AnalysisService.AnalysisResult call() {
                updateMessage("Analyzing signal...");
                return analysisService.analyzeSignal(currentSignal);
            }
        };
        
        analysisTask.setOnSucceeded(event -> {
            currentResult = analysisTask.getValue();
            displayResults(currentResult);
            clearResultsButton.setDisable(false);
            exportPdfButton.setDisable(false);
            updateStatus(String.format("Analysis complete. Found %d anomalies.", 
                currentResult.anomalies().size()));
            progressBar.setVisible(false);
        });
        
        analysisTask.setOnFailed(event -> {
            showError("Analysis failed", analysisTask.getException().getMessage());
            progressBar.setVisible(false);
        });
        
        progressBar.setVisible(true);
        updateStatus("Running analysis...");
        
        new Thread(analysisTask).start();
    }
    
    /**
     * Displays analysis results.
     */
    private void displayResults(AnalysisService.AnalysisResult result) {
        // Update metrics
        double heartRate = result.getAverageHeartRate();
        heartRateLabel.setText(String.format("%.1f BPM", heartRate));
        
        // Set heart rate status
        if (heartRate > 100) {
            heartRateStatus.setText("⚠️ High");
            heartRateStatus.setStyle("-fx-text-fill: #E74C3C;");
        } else if (heartRate < 60) {
            heartRateStatus.setText("⚠️ Low");
            heartRateStatus.setStyle("-fx-text-fill: #F39C12;");
        } else {
            heartRateStatus.setText("✓ Normal");
            heartRateStatus.setStyle("-fx-text-fill: #27AE60;");
        }
        
        rPeaksCountLabel.setText(String.valueOf(result.rPeakIndices().size()));
        processingTimeLabel.setText(result.processingTimeMs() + " ms");
        
        // Calculate average RR interval
        if (result.rPeakIndices().size() >= 2) {
            double avgRR = 60.0 / heartRate;
            rrIntervalLabel.setText(String.format("%.0f ms", avgRR * 1000));
        } else {
            rrIntervalLabel.setText("N/A");
        }
        
        // Add R-peaks to chart
        addRPeaksToChart(result);
        
        // Populate anomaly table
        populateAnomalyTable(result.anomalies());
    }
    
    /**
     * Adds R-peak markers to the chart.
     */
    private void addRPeaksToChart(AnalysisService.AnalysisResult result) {
        XYChart.Series<Number, Number> peakSeries = new XYChart.Series<>();
        peakSeries.setName("R-Peaks");
        
        List<EcgDataPoint> dataPoints = result.signal().dataPoints();
        for (Integer peakIndex : result.rPeakIndices()) {
            if (peakIndex < dataPoints.size()) {
                EcgDataPoint peak = dataPoints.get(peakIndex);
                peakSeries.getData().add(new XYChart.Data<>(peak.time(), peak.amplitude()));
            }
        }
        
        ecgChart.getData().add(peakSeries);
    }
    
    /**
     * Populates the anomaly results table.
     */
    private void populateAnomalyTable(List<Anomaly> anomalies) {
        ObservableList<AnomalyRow> rows = FXCollections.observableArrayList();
        
        for (Anomaly anomaly : anomalies) {
            rows.add(new AnomalyRow(anomaly));
        }
        
        anomalyTable.setItems(rows);
    }
    
    /**
     * Highlights an anomaly location in the chart.
     */
    private void highlightAnomalyInChart(AnomalyRow anomalyRow) {
        logger.info("Highlighting anomaly at: {}", anomalyRow.getTimestamp());
        updateStatus("Selected anomaly at " + anomalyRow.getTimestamp() + ": " + anomalyRow.getType());
    }
    
    /**
     * Handles Export PDF button click.
     */
    @FXML
    private void handleExportPdf() {
        if (currentResult == null) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export PDF Report");
        fileChooser.setInitialFileName("ecg_analysis_report.pdf");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showSaveDialog(exportPdfButton.getScene().getWindow());
        if (file != null) {
            try {
                pdfExporter.exportReport(currentResult, ecgChart, file, 
                    currentFilePath != null ? currentFilePath : "Unknown");
                updateStatus("PDF report exported successfully to " + file.getName());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("PDF report saved to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                logger.error("Failed to export PDF", e);
                showError("PDF Export Failed", e.getMessage());
            }
        }
    }
    
    /**
     * Handles Export Chart button click.
     */
    @FXML
    private void handleExportChart() {
        if (ecgChart.getData().isEmpty()) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Chart Image");
        fileChooser.setInitialFileName("ecg_chart.png");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Image", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Image", "*.jpg")
        );
        
        File file = fileChooser.showSaveDialog(exportChartButton.getScene().getWindow());
        if (file != null) {
            try {
                if (file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg")) {
                    ChartExporter.exportChartAsJpg(ecgChart, file);
                } else {
                    ChartExporter.exportChartAsPng(ecgChart, file);
                }
                updateStatus("Chart exported successfully to " + file.getName());
            } catch (Exception e) {
                logger.error("Failed to export chart", e);
                showError("Chart Export Failed", e.getMessage());
            }
        }
    }
    
    /**
     * Handles theme toggle button click.
     */
    @FXML
    private void handleToggleTheme() {
        String newTheme = userPreferences.isDarkMode() ? "light" : "dark";
        userPreferences.setTheme(newTheme);
        applyTheme(newTheme);
    }
    
    /**
     * Applies the selected theme.
     */
    private void applyTheme(String theme) {
        javafx.scene.Scene scene = themeToggleButton.getScene();
        if (scene == null) {
            logger.warn("Cannot apply theme: scene not available yet");
            return;
        }
        
        scene.getStylesheets().clear();
        
        if ("dark".equals(theme)) {
            try {
                scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
                themeToggleButton.setText("☀️");
                logger.info("Applied dark theme");
            } catch (Exception e) {
                logger.error("Failed to load dark theme CSS", e);
            }
        } else {
            try {
                // Try to load light theme CSS, fall back to default if not found
                var lightThemeUrl = getClass().getResource("/styles/application.css");
                if (lightThemeUrl != null) {
                    scene.getStylesheets().add(lightThemeUrl.toExternalForm());
                }
                themeToggleButton.setText("🌙");
                logger.info("Applied light theme");
            } catch (Exception e) {
                logger.error("Failed to load light theme CSS", e);
            }
        }
    }
    
    /**
     * Switches to rule-based detector.
     */
    private void switchToRuleBasedDetector() {
        analysisService.setAnomalyDetector(new RuleBasedDetector());
        updateStatus("Switched to Rule-Based Detection");
        logger.info("Switched to rule-based detector");
    }
    
    /**
     * Switches to ML-based detector.
     */
    private void switchToMLDetector() {
        analysisService.setAnomalyDetector(new MLBasedDetector());
        updateStatus("Switched to ML-Based Detection (DL4J)");
        logger.info("Switched to ML-based detector");
    }
    
    /**
     * Handles Clear Results button click.
     */
    @FXML
    private void handleClearResults() {
        ecgChart.getData().clear();
        anomalyTable.getItems().clear();
        heartRateLabel.setText("--");
        heartRateStatus.setText("--");
        rrIntervalLabel.setText("--");
        rPeaksCountLabel.setText("--");
        processingTimeLabel.setText("--");
        fileInfoLabel.setText("No file loaded");
        
        currentSignal = null;
        currentResult = null;
        currentFilePath = null;
        
        runAnalysisButton.setDisable(true);
        clearResultsButton.setDisable(true);
        exportPdfButton.setDisable(true);
        exportChartButton.setDisable(true);
        
        updateStatus("Results cleared. Load a new file to begin.");
    }
    
    /**
     * Updates the status bar message.
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    /**
     * Shows an error dialog.
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * TableView row wrapper for Anomaly.
     */
    public static class AnomalyRow {
        private final Anomaly anomaly;
        
        public AnomalyRow(Anomaly anomaly) {
            this.anomaly = anomaly;
        }
        
        public String getTimestamp() {
            return String.format("%.2fs", anomaly.timestamp());
        }
        
        public String getType() {
            return anomaly.type().getDisplayName();
        }
        
        public String getSeverity() {
            return anomaly.severity().getIcon() + " " + anomaly.severity().getLabel();
        }
        
        public String getConfidence() {
            return anomaly.getConfidencePercent() + "%";
        }
        
        public String getDetails() {
            return anomaly.details();
        }
    }
}
