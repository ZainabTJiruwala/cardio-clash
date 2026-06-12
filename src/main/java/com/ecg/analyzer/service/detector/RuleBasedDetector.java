package com.ecg.analyzer.service.detector;

import com.ecg.analyzer.model.Anomaly;
import com.ecg.analyzer.model.Anomaly.Severity;
import com.ecg.analyzer.model.AnomalyType;
import com.ecg.analyzer.model.EcgDataPoint;
import com.ecg.analyzer.model.EcgSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based anomaly detector using heart rate and ECG feature analysis.
 * Detects: Tachycardia, Bradycardia, and simple arrhythmias.
 */
public class RuleBasedDetector implements AnomalyDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleBasedDetector.class);
    
    // Heart rate thresholds
    private static final double TACHYCARDIA_THRESHOLD = 100.0; // BPM
    private static final double BRADYCARDIA_THRESHOLD = 60.0;  // BPM
    private static final double SEVERE_TACHYCARDIA_THRESHOLD = 150.0;
    private static final double SEVERE_BRADYCARDIA_THRESHOLD = 40.0;
    
    // Variability thresholds
    private static final double ARRHYTHMIA_RR_VARIANCE_THRESHOLD = 0.3; // 30% variance
    
    @Override
    public List<Anomaly> detectAnomalies(EcgSignal signal, List<Integer> rPeakIndices) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        if (rPeakIndices == null || rPeakIndices.size() < 2) {
            logger.warn("Insufficient R-peaks for analysis: {}", 
                rPeakIndices != null ? rPeakIndices.size() : 0);
            return anomalies;
        }
        
        // Calculate RR intervals and heart rate
        List<Double> rrIntervals = calculateRRIntervals(signal, rPeakIndices);
        double averageHeartRate = calculateAverageHeartRate(rrIntervals);
        
        logger.info("Average heart rate: {:.2f} BPM", averageHeartRate);
        
        // Detect tachycardia
        if (averageHeartRate > TACHYCARDIA_THRESHOLD) {
            Severity severity = averageHeartRate > SEVERE_TACHYCARDIA_THRESHOLD 
                ? Severity.HIGH : Severity.MEDIUM;
            anomalies.add(new Anomaly(
                AnomalyType.TACHYCARDIA,
                signal.dataPoints().get(0).time(),
                severity,
                0.95,
                String.format("Average HR: %.1f BPM", averageHeartRate)
            ));
        }
        
        // Detect bradycardia
        if (averageHeartRate < BRADYCARDIA_THRESHOLD) {
            Severity severity = averageHeartRate < SEVERE_BRADYCARDIA_THRESHOLD 
                ? Severity.HIGH : Severity.MEDIUM;
            anomalies.add(new Anomaly(
                AnomalyType.BRADYCARDIA,
                signal.dataPoints().get(0).time(),
                severity,
                0.95,
                String.format("Average HR: %.1f BPM", averageHeartRate)
            ));
        }
        
        // Detect arrhythmia (irregular heart rhythm)
        double rrVariance = calculateVariance(rrIntervals);
        double rrMean = rrIntervals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double coefficientOfVariation = Math.sqrt(rrVariance) / rrMean;
        
        if (coefficientOfVariation > ARRHYTHMIA_RR_VARIANCE_THRESHOLD) {
            anomalies.add(new Anomaly(
                AnomalyType.ARRHYTHMIA,
                signal.dataPoints().get(0).time(),
                Severity.MEDIUM,
                0.75,
                String.format("RR interval variability: %.1f%%", coefficientOfVariation * 100)
            ));
        }
        
        // Detect premature contractions (very short RR intervals)
        detectPrematureContractions(signal, rrIntervals, rPeakIndices, anomalies);
        
        logger.info("Detected {} anomalies", anomalies.size());
        return anomalies;
    }
    
    /**
     * Calculates RR intervals (time between consecutive R-peaks).
     */
    private List<Double> calculateRRIntervals(EcgSignal signal, List<Integer> rPeakIndices) {
        List<Double> rrIntervals = new ArrayList<>();
        List<EcgDataPoint> dataPoints = signal.dataPoints();
        
        for (int i = 1; i < rPeakIndices.size(); i++) {
            double time1 = dataPoints.get(rPeakIndices.get(i - 1)).time();
            double time2 = dataPoints.get(rPeakIndices.get(i)).time();
            rrIntervals.add(time2 - time1);
        }
        
        return rrIntervals;
    }
    
    /**
     * Calculates average heart rate from RR intervals.
     */
    private double calculateAverageHeartRate(List<Double> rrIntervals) {
        if (rrIntervals.isEmpty()) return 0.0;
        
        double avgRRInterval = rrIntervals.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Convert RR interval (seconds) to heart rate (beats per minute)
        return avgRRInterval > 0 ? 60.0 / avgRRInterval : 0.0;
    }
    
    /**
     * Calculates variance of RR intervals.
     */
    private double calculateVariance(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        
        return variance;
    }
    
    /**
     * Detects premature contractions (abnormally short RR intervals).
     */
    private void detectPrematureContractions(EcgSignal signal, List<Double> rrIntervals, 
                                            List<Integer> rPeakIndices, List<Anomaly> anomalies) {
        if (rrIntervals.size() < 3) return;
        
        double meanRR = rrIntervals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        for (int i = 0; i < rrIntervals.size(); i++) {
            // If RR interval is less than 60% of mean, it might be premature
            if (rrIntervals.get(i) < meanRR * 0.6) {
                double timestamp = signal.dataPoints().get(rPeakIndices.get(i + 1)).time();
                anomalies.add(new Anomaly(
                    AnomalyType.PREMATURE_CONTRACTION,
                    timestamp,
                    Severity.LOW,
                    0.70,
                    String.format("RR interval: %.3fs (%.1f%% of normal)", 
                        rrIntervals.get(i), (rrIntervals.get(i) / meanRR) * 100)
                ));
            }
        }
    }
    
    @Override
    public String getDetectorName() {
        return "Rule-Based Detector";
    }
    
    @Override
    public String getDescription() {
        return "Detects tachycardia, bradycardia, arrhythmias, and premature contractions using heart rate analysis";
    }
}
