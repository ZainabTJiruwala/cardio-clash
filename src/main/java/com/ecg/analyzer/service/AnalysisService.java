package com.ecg.analyzer.service;

import com.ecg.analyzer.model.Anomaly;
import com.ecg.analyzer.model.EcgSignal;
import com.ecg.analyzer.repository.DataValidationException;
import com.ecg.analyzer.repository.EcgDataRepository;
import com.ecg.analyzer.service.detector.AnomalyDetector;
import com.ecg.analyzer.service.preprocessing.RPeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Main service orchestrating ECG analysis workflow.
 * Coordinates data loading, preprocessing, and anomaly detection.
 */
public class AnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);
    
    private final EcgDataRepository repository;
    private final RPeakDetector rPeakDetector;
    private AnomalyDetector anomalyDetector;
    
    public AnalysisService(EcgDataRepository repository, AnomalyDetector anomalyDetector) {
        this.repository = repository;
        this.anomalyDetector = anomalyDetector;
        this.rPeakDetector = new RPeakDetector();
    }
    
    /**
     * Sets the anomaly detector to use.
     */
    public void setAnomalyDetector(AnomalyDetector detector) {
        this.anomalyDetector = detector;
        logger.info("Switched to detector: {}", detector.getDetectorName());
    }
    
    /**
     * Loads ECG data from a file.
     */
    public EcgSignal loadEcgData(Path filePath) throws IOException, DataValidationException {
        logger.info("Loading ECG data from: {}", filePath);
        return repository.loadFromFile(filePath);
    }
    
    /**
     * Analyzes an ECG signal and returns detected anomalies.
     */
    public AnalysisResult analyzeSignal(EcgSignal signal) {
        logger.info("Starting analysis of signal: {}", signal.recordId());
        
        long startTime = System.currentTimeMillis();
        
        // Step 1: Detect R-peaks
        List<Integer> rPeakIndices = rPeakDetector.detectRPeaks(signal);
        
        // Step 2: Detect anomalies
        List<Anomaly> anomalies = anomalyDetector.detectAnomalies(signal, rPeakIndices);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Analysis completed in {}ms. Found {} anomalies", duration, anomalies.size());
        
        return new AnalysisResult(signal, rPeakIndices, anomalies, duration);
    }
    
    /**
     * Complete workflow: load and analyze.
     */
    public AnalysisResult loadAndAnalyze(Path filePath) 
            throws IOException, DataValidationException {
        EcgSignal signal = loadEcgData(filePath);
        return analyzeSignal(signal);
    }
    
    /**
     * Result of ECG analysis containing all computed data.
     */
    public record AnalysisResult(
        EcgSignal signal,
        List<Integer> rPeakIndices,
        List<Anomaly> anomalies,
        long processingTimeMs
    ) {
        /**
         * Calculates average heart rate from R-peaks.
         */
        public double getAverageHeartRate() {
            if (rPeakIndices.size() < 2) return 0.0;
            
            double firstPeakTime = signal.dataPoints().get(rPeakIndices.get(0)).time();
            double lastPeakTime = signal.dataPoints()
                .get(rPeakIndices.get(rPeakIndices.size() - 1)).time();
            double duration = lastPeakTime - firstPeakTime;
            
            if (duration <= 0) return 0.0;
            
            int beatCount = rPeakIndices.size() - 1;
            return (beatCount / duration) * 60.0; // Convert to BPM
        }
        
        /**
         * Gets count of anomalies by severity.
         */
        public long getAnomalyCountBySeverity(Anomaly.Severity severity) {
            return anomalies.stream()
                .filter(a -> a.severity() == severity)
                .count();
        }
    }
}
