package com.ecg.analyzer.service.detector;

import com.ecg.analyzer.model.Anomaly;
import com.ecg.analyzer.model.EcgSignal;
import java.util.List;

/**
 * Interface for ECG anomaly detection algorithms.
 * Implementations can use rule-based logic, ML models, or hybrid approaches.
 */
public interface AnomalyDetector {
    
    /**
     * Analyzes an ECG signal and detects anomalies.
     * 
     * @param signal The ECG signal to analyze
     * @param rPeakIndices Indices of detected R-peaks (can be null if not needed)
     * @return List of detected anomalies
     */
    List<Anomaly> detectAnomalies(EcgSignal signal, List<Integer> rPeakIndices);
    
    /**
     * Gets the name of this detector.
     */
    String getDetectorName();
    
    /**
     * Gets a description of what this detector can identify.
     */
    String getDescription();
}
