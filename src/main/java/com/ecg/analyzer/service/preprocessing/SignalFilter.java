package com.ecg.analyzer.service.preprocessing;

import com.ecg.analyzer.model.EcgDataPoint;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Signal filtering utilities for ECG preprocessing.
 * Implements various filters for noise reduction and signal enhancement.
 */
public class SignalFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalFilter.class);
    
    /**
     * Applies a bandpass filter (5-15 Hz) for QRS complex enhancement.
     * This is the first step in the Pan-Tompkins algorithm.
     */
    public static List<EcgDataPoint> bandpassFilter(List<EcgDataPoint> dataPoints, int samplingRate) {
        logger.debug("Applying bandpass filter (5-15 Hz)");
        
        // Apply high-pass filter at 5 Hz followed by low-pass filter at 15 Hz
        List<EcgDataPoint> highPassed = highPassFilter(dataPoints, samplingRate, 5.0);
        return lowPassFilter(highPassed, samplingRate, 15.0);
    }
    
    /**
     * Removes baseline wander using a high-pass filter at 0.5 Hz.
     */
    public static List<EcgDataPoint> removeBaselineWander(List<EcgDataPoint> dataPoints, int samplingRate) {
        logger.debug("Removing baseline wander");
        return highPassFilter(dataPoints, samplingRate, 0.5);
    }
    
    /**
     * Reduces high-frequency noise using a low-pass filter at 40 Hz.
     */
    public static List<EcgDataPoint> reduceNoise(List<EcgDataPoint> dataPoints, int samplingRate) {
        logger.debug("Applying noise reduction filter");
        return lowPassFilter(dataPoints, samplingRate, 40.0);
    }
    
    /**
     * Simple high-pass filter implementation using difference equation.
     */
    private static List<EcgDataPoint> highPassFilter(List<EcgDataPoint> dataPoints, int samplingRate, double cutoffFreq) {
        List<EcgDataPoint> filtered = new ArrayList<>();
        
        // Calculate filter coefficient
        double RC = 1.0 / (2.0 * Math.PI * cutoffFreq);
        double dt = 1.0 / samplingRate;
        double alpha = RC / (RC + dt);
        
        double prevInput = dataPoints.get(0).amplitude();
        double prevOutput = 0.0;
        
        for (int i = 0; i < dataPoints.size(); i++) {
            EcgDataPoint point = dataPoints.get(i);
            double input = point.amplitude();
            
            // High-pass filter: y[n] = alpha * (y[n-1] + x[n] - x[n-1])
            double output = alpha * (prevOutput + input - prevInput);
            
            filtered.add(new EcgDataPoint(point.time(), output));
            
            prevInput = input;
            prevOutput = output;
        }
        
        return filtered;
    }
    
    /**
     * Simple low-pass filter implementation using exponential moving average.
     */
    private static List<EcgDataPoint> lowPassFilter(List<EcgDataPoint> dataPoints, int samplingRate, double cutoffFreq) {
        List<EcgDataPoint> filtered = new ArrayList<>();
        
        // Calculate smoothing factor
        double RC = 1.0 / (2.0 * Math.PI * cutoffFreq);
        double dt = 1.0 / samplingRate;
        double alpha = dt / (RC + dt);
        
        double smoothedValue = dataPoints.get(0).amplitude();
        
        for (EcgDataPoint point : dataPoints) {
            // Exponential moving average: y[n] = alpha * x[n] + (1 - alpha) * y[n-1]
            smoothedValue = alpha * point.amplitude() + (1 - alpha) * smoothedValue;
            filtered.add(new EcgDataPoint(point.time(), smoothedValue));
        }
        
        return filtered;
    }
    
    /**
     * Applies derivative filter to emphasize QRS slope (Pan-Tompkins step 2).
     * Uses 5-point derivative: y[n] = (1/8T)(-x[n-2] - 2x[n-1] + 2x[n+1] + x[n+2])
     */
    public static List<EcgDataPoint> derivativeFilter(List<EcgDataPoint> dataPoints, int samplingRate) {
        logger.debug("Applying derivative filter");
        List<EcgDataPoint> filtered = new ArrayList<>();
        
        double dt = 1.0 / samplingRate;
        
        for (int i = 0; i < dataPoints.size(); i++) {
            double derivative;
            
            if (i < 2 || i >= dataPoints.size() - 2) {
                // Edge cases: use simple derivative
                derivative = 0.0;
            } else {
                // 5-point derivative
                double xMinus2 = dataPoints.get(i - 2).amplitude();
                double xMinus1 = dataPoints.get(i - 1).amplitude();
                double xPlus1 = dataPoints.get(i + 1).amplitude();
                double xPlus2 = dataPoints.get(i + 2).amplitude();
                
                derivative = (-xMinus2 - 2 * xMinus1 + 2 * xPlus1 + xPlus2) / (8 * dt);
            }
            
            filtered.add(new EcgDataPoint(dataPoints.get(i).time(), derivative));
        }
        
        return filtered;
    }
    
    /**
     * Squares the signal to make all values positive and amplify higher frequencies (Pan-Tompkins step 3).
     */
    public static List<EcgDataPoint> squareSignal(List<EcgDataPoint> dataPoints) {
        logger.debug("Squaring signal");
        List<EcgDataPoint> squared = new ArrayList<>();
        
        for (EcgDataPoint point : dataPoints) {
            double value = point.amplitude();
            squared.add(new EcgDataPoint(point.time(), value * value));
        }
        
        return squared;
    }
    
    /**
     * Moving window integration to obtain waveform feature information (Pan-Tompkins step 4).
     * Window size is typically 150ms (about 0.15 * samplingRate samples).
     */
    public static List<EcgDataPoint> movingWindowIntegration(List<EcgDataPoint> dataPoints, int windowSize) {
        logger.debug("Applying moving window integration with window size: {}", windowSize);
        List<EcgDataPoint> integrated = new ArrayList<>();
        
        for (int i = 0; i < dataPoints.size(); i++) {
            double sum = 0.0;
            int count = 0;
            
            // Calculate average over window
            for (int j = Math.max(0, i - windowSize + 1); j <= i; j++) {
                sum += dataPoints.get(j).amplitude();
                count++;
            }
            
            double average = sum / count;
            integrated.add(new EcgDataPoint(dataPoints.get(i).time(), average));
        }
        
        return integrated;
    }
    
    /**
     * Normalizes signal to range [0, 1].
     */
    public static List<EcgDataPoint> normalize(List<EcgDataPoint> dataPoints) {
        double min = dataPoints.stream().mapToDouble(EcgDataPoint::amplitude).min().orElse(0.0);
        double max = dataPoints.stream().mapToDouble(EcgDataPoint::amplitude).max().orElse(1.0);
        double range = max - min;
        
        if (range == 0) {
            return dataPoints;
        }
        
        List<EcgDataPoint> normalized = new ArrayList<>();
        for (EcgDataPoint point : dataPoints) {
            double normalizedValue = (point.amplitude() - min) / range;
            normalized.add(new EcgDataPoint(point.time(), normalizedValue));
        }
        
        return normalized;
    }
}
