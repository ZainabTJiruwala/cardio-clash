package com.ecg.analyzer.service.preprocessing;

import com.ecg.analyzer.model.EcgDataPoint;
import com.ecg.analyzer.model.EcgSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects R-peaks in ECG signals using Pan-Tompkins algorithm.
 * R-peaks are the highest points in the QRS complex.
 */
public class RPeakDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(RPeakDetector.class);
    
    /**
     * Detects R-peaks in the ECG signal using the Pan-Tompkins algorithm.
     * This is the industry-standard algorithm with the following steps:
     * 1. Bandpass filter (5-15 Hz)
     * 2. Derivative filter
     * 3. Squaring
     * 4. Moving window integration
     * 5. Adaptive thresholding
     * 
     * @param signal The ECG signal to analyze
     * @return List of indices where R-peaks occur
     */
    public List<Integer> detectRPeaks(EcgSignal signal) {
        logger.info("Detecting R-peaks using Pan-Tompkins algorithm: {}", signal.recordId());
        
        List<EcgDataPoint> dataPoints = signal.dataPoints();
        List<Integer> rPeakIndices = new ArrayList<>();
        
        if (dataPoints.size() < 10) {
            logger.warn("Signal too short for R-peak detection");
            return rPeakIndices;
        }
        
        // Step 1: Bandpass filter (5-15 Hz) - removes baseline wander and high frequency noise
        List<EcgDataPoint> filtered = SignalFilter.bandpassFilter(dataPoints, signal.samplingRate());
        
        // Step 2: Derivative filter - emphasizes QRS complex slope
        List<EcgDataPoint> derivative = SignalFilter.derivativeFilter(filtered, signal.samplingRate());
        
        // Step 3: Squaring - makes all values positive and amplifies higher frequencies
        List<EcgDataPoint> squared = SignalFilter.squareSignal(derivative);
        
        // Step 4: Moving window integration - extracts waveform feature information
        int windowSize = (int) (0.15 * signal.samplingRate()); // 150ms window
        List<EcgDataPoint> integrated = SignalFilter.movingWindowIntegration(squared, windowSize);
        
        // Step 5: Adaptive thresholding for R-peak detection
        rPeakIndices = adaptiveThresholding(integrated, signal.samplingRate());
        
        logger.info("Detected {} R-peaks using Pan-Tompkins algorithm", rPeakIndices.size());
        return rPeakIndices;
    }
    
    /**
     * Applies adaptive thresholding to detect R-peaks.
     * Uses dual thresholds that adapt based on signal and noise levels.
     */
    private List<Integer> adaptiveThresholding(List<EcgDataPoint> signal, int samplingRate) {
        List<Integer> rPeakIndices = new ArrayList<>();
        
        // Initialize threshold parameters
        double signalPeak = 0.0;
        double noisePeak = 0.0;
        double threshold1 = 0.0;
        double threshold2 = 0.0;
        
        // Learning phase - establish initial thresholds
        int learningPhaseSize = Math.min(2 * samplingRate, signal.size()); // First 2 seconds
        for (int i = 0; i < learningPhaseSize; i++) {
            double value = signal.get(i).amplitude();
            if (value > signalPeak) {
                signalPeak = value;
            }
        }
        
        // Initialize thresholds
        noisePeak = signalPeak * 0.1;
        threshold1 = noisePeak + 0.3125 * (signalPeak - noisePeak);
        threshold2 = 0.5 * threshold1;
        
        // Refractory period (200ms minimum between R-peaks)
        int refractoryPeriod = (int) (0.2 * samplingRate);
        int lastPeakIndex = -refractoryPeriod;
        
        // Find peaks using adaptive thresholds
        int searchWindowSize = samplingRate / 6; // ~0.15s window
        
        for (int i = searchWindowSize; i < signal.size() - searchWindowSize; i++) {
            double currentValue = signal.get(i).amplitude();
            
            // Check if this is a local maximum
            if (isLocalMaximum(signal, i, searchWindowSize)) {
                // Check if it exceeds threshold and respects refractory period
                if ((currentValue > threshold1 || currentValue > threshold2) && 
                    (i - lastPeakIndex) > refractoryPeriod) {
                    
                    rPeakIndices.add(i);
                    lastPeakIndex = i;
                    
                    // Update signal peak estimate
                    signalPeak = 0.125 * currentValue + 0.875 * signalPeak;
                    
                } else if (currentValue > noisePeak && currentValue <= threshold1) {
                    // Update noise peak estimate
                    noisePeak = 0.125 * currentValue + 0.875 * noisePeak;
                }
                
                // Update thresholds
                threshold1 = noisePeak + 0.3125 * (signalPeak - noisePeak);
                threshold2 = 0.5 * threshold1;
            }
        }
        
        return rPeakIndices;
    }
    
    /**
     * Checks if a point is a local maximum within a window.
     */
    private boolean isLocalMaximum(List<EcgDataPoint> dataPoints, int index, int windowSize) {
        double value = dataPoints.get(index).amplitude();
        
        // Check left side
        for (int i = index - windowSize; i < index; i++) {
            if (dataPoints.get(i).amplitude() > value) {
                return false;
            }
        }
        
        // Check right side
        for (int i = index + 1; i <= index + windowSize; i++) {
            if (dataPoints.get(i).amplitude() > value) {
                return false;
            }
        }
        
        return true;
    }
}
