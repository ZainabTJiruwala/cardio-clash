package com.ecg.analyzer.service.ml;

import com.ecg.analyzer.model.EcgDataPoint;
import com.ecg.analyzer.model.EcgSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts features from ECG signals for machine learning models.
 * Features include ST segment characteristics, QRS complex duration,
 * T-wave morphology, and frequency domain features.
 */
public class FeatureExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureExtractor.class);
    
    /**
     * Extracts a comprehensive feature vector from an ECG signal and R-peak locations.
     * 
     * @param signal The ECG signal
     * @param rPeakIndices Indices of detected R-peaks
     * @return Feature vector as double array
     */
    public double[] extractFeatures(EcgSignal signal, List<Integer> rPeakIndices) {
        logger.debug("Extracting ML features from signal");
        
        List<Double> features = new ArrayList<>();
        
        // Basic statistics
        features.addAll(extractStatisticalFeatures(signal));
        
        // R-peak features
        features.addAll(extractRPeakFeatures(signal, rPeakIndices));
        
        // Morphological features (ST segment, QRS complex, T-wave)
        features.addAll(extractMorphologicalFeatures(signal, rPeakIndices));
        
        // Frequency domain features
        features.addAll(extractFrequencyFeatures(signal));
        
        // Convert to array
        double[] featureArray = features.stream().mapToDouble(Double::doubleValue).toArray();
        logger.debug("Extracted {} features", featureArray.length);
        
        return featureArray;
    }
    
    /**
     * Extracts basic statistical features from the signal.
     */
    private List<Double> extractStatisticalFeatures(EcgSignal signal) {
        List<Double> features = new ArrayList<>();
        List<EcgDataPoint> dataPoints = signal.dataPoints();
        
        // Mean amplitude
        double mean = dataPoints.stream()
            .mapToDouble(EcgDataPoint::amplitude)
            .average()
            .orElse(0.0);
        features.add(mean);
        
        // Standard deviation
        double variance = dataPoints.stream()
            .mapToDouble(p -> Math.pow(p.amplitude() - mean, 2))
            .average()
            .orElse(0.0);
        features.add(Math.sqrt(variance));
        
        // Min and Max
        double min = dataPoints.stream().mapToDouble(EcgDataPoint::amplitude).min().orElse(0.0);
        double max = dataPoints.stream().mapToDouble(EcgDataPoint::amplitude).max().orElse(0.0);
        features.add(min);
        features.add(max);
        features.add(max - min); // Range
        
        return features;
    }
    
    /**
     * Extracts features related to R-peaks and heart rate variability.
     */
    private List<Double> extractRPeakFeatures(EcgSignal signal, List<Integer> rPeakIndices) {
        List<Double> features = new ArrayList<>();
        
        if (rPeakIndices.size() < 2) {
            // Not enough R-peaks, add zeros
            for (int i = 0; i < 5; i++) {
                features.add(0.0);
            }
            return features;
        }
        
        List<EcgDataPoint> dataPoints = signal.dataPoints();
        
        // Average R-peak amplitude
        double avgRPeakAmplitude = rPeakIndices.stream()
            .mapToDouble(idx -> dataPoints.get(idx).amplitude())
            .average()
            .orElse(0.0);
        features.add(avgRPeakAmplitude);
        
        // RR intervals
        List<Double> rrIntervals = new ArrayList<>();
        for (int i = 1; i < rPeakIndices.size(); i++) {
            double rrInterval = dataPoints.get(rPeakIndices.get(i)).time() - 
                               dataPoints.get(rPeakIndices.get(i - 1)).time();
            rrIntervals.add(rrInterval);
        }
        
        // Mean RR interval
        double meanRR = rrIntervals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        features.add(meanRR);
        
        // SDNN (Standard Deviation of RR intervals) - HRV measure
        double rrVariance = rrIntervals.stream()
            .mapToDouble(rr -> Math.pow(rr - meanRR, 2))
            .average()
            .orElse(0.0);
        features.add(Math.sqrt(rrVariance));
        
        // RMSSD (Root Mean Square of Successive Differences)
        double rmssd = 0.0;
        if (rrIntervals.size() > 1) {
            double sumSquaredDiffs = 0.0;
            for (int i = 1; i < rrIntervals.size(); i++) {
                double diff = rrIntervals.get(i) - rrIntervals.get(i - 1);
                sumSquaredDiffs += diff * diff;
            }
            rmssd = Math.sqrt(sumSquaredDiffs / (rrIntervals.size() - 1));
        }
        features.add(rmssd);
        
        // Heart rate
        double heartRate = 60.0 / meanRR;
        features.add(heartRate);
        
        return features;
    }
    
    /**
     * Extracts morphological features (ST segment, QRS complex, T-wave).
     */
    private List<Double> extractMorphologicalFeatures(EcgSignal signal, List<Integer> rPeakIndices) {
        List<Double> features = new ArrayList<>();
        
        if (rPeakIndices.isEmpty()) {
            // Add zeros if no R-peaks
            for (int i = 0; i < 6; i++) {
                features.add(0.0);
            }
            return features;
        }
        
        List<EcgDataPoint> dataPoints = signal.dataPoints();
        int samplingRate = signal.samplingRate();
        
        // Analyze first few heartbeats
        int beatsToAnalyze = Math.min(5, rPeakIndices.size());
        
        double avgQrsWidth = 0.0;
        double avgStElevation = 0.0;
        double avgTWaveAmplitude = 0.0;
        
        for (int i = 0; i < beatsToAnalyze && i < rPeakIndices.size() - 1; i++) {
            int rPeakIdx = rPeakIndices.get(i);
            
            // QRS complex width (typically 60-100ms before and after R-peak)
            int qrsStart = Math.max(0, rPeakIdx - (int)(0.06 * samplingRate));
            int qrsEnd = Math.min(dataPoints.size() - 1, rPeakIdx + (int)(0.06 * samplingRate));
            avgQrsWidth += (qrsEnd - qrsStart) / (double) samplingRate;
            
            // Find baseline (isoelectric line) - average of PR segment before QRS
            int prStart = Math.max(0, rPeakIdx - (int)(0.2 * samplingRate));
            int prEnd = Math.max(0, rPeakIdx - (int)(0.08 * samplingRate));
            double baseline = 0.0;
            if (prEnd > prStart) {
                for (int j = prStart; j < prEnd; j++) {
                    baseline += dataPoints.get(j).amplitude();
                }
                baseline /= (prEnd - prStart);
            }
            
            // ST segment (60-100ms after QRS end)
            int stSegmentIdx = Math.min(dataPoints.size() - 1, rPeakIdx + (int)(0.08 * samplingRate));
            if (stSegmentIdx < dataPoints.size()) {
                double stAmp = dataPoints.get(stSegmentIdx).amplitude();
                // ST elevation is measured relative to baseline, not R-peak
                avgStElevation += (stAmp - baseline);
            }
            
            // T-wave (200-400ms after R-peak)
            int tWaveIdx = Math.min(dataPoints.size() - 1, rPeakIdx + (int)(0.3 * samplingRate));
            if (tWaveIdx < dataPoints.size()) {
                avgTWaveAmplitude += dataPoints.get(tWaveIdx).amplitude();
            }
        }
        
        avgQrsWidth /= beatsToAnalyze;
        avgStElevation /= beatsToAnalyze;
        avgTWaveAmplitude /= beatsToAnalyze;
        
        features.add(avgQrsWidth);
        features.add(avgStElevation);
        features.add(avgTWaveAmplitude);
        
        // ST segment amplitude (indicator for MI)
        features.add(Math.abs(avgStElevation));
        
        // QRS amplitude
        double avgQrsAmplitude = rPeakIndices.stream()
            .mapToDouble(idx -> dataPoints.get(idx).amplitude())
            .average()
            .orElse(0.0);
        features.add(avgQrsAmplitude);
        
        // T/R ratio (T-wave to R-peak ratio)
        double trRatio = avgQrsAmplitude != 0 ? avgTWaveAmplitude / avgQrsAmplitude : 0.0;
        features.add(trRatio);
        
        return features;
    }
    
    /**
     * Extracts frequency domain features using simple power estimation.
     */
    private List<Double> extractFrequencyFeatures(EcgSignal signal) {
        List<Double> features = new ArrayList<>();
        List<EcgDataPoint> dataPoints = signal.dataPoints();
        
        // Calculate power in different frequency bands
        // Low frequency (0.04-0.15 Hz) - power
        // High frequency (0.15-0.4 Hz) - power
        // This is a simplified approach without full FFT
        
        // Zero crossing rate (proxy for dominant frequency)
        int zeroCrossings = 0;
        double mean = dataPoints.stream().mapToDouble(EcgDataPoint::amplitude).average().orElse(0.0);
        
        for (int i = 1; i < dataPoints.size(); i++) {
            if ((dataPoints.get(i).amplitude() - mean) * (dataPoints.get(i - 1).amplitude() - mean) < 0) {
                zeroCrossings++;
            }
        }
        
        double crossingRate = zeroCrossings / signal.duration();
        features.add(crossingRate);
        
        // Energy (sum of squared amplitudes)
        double energy = dataPoints.stream()
            .mapToDouble(p -> p.amplitude() * p.amplitude())
            .sum();
        features.add(energy);
        
        // Spectral entropy (simplified - based on amplitude distribution)
        double entropy = calculateSimpleEntropy(dataPoints);
        features.add(entropy);
        
        return features;
    }
    
    /**
     * Calculates a simplified entropy measure based on amplitude distribution.
     */
    private double calculateSimpleEntropy(List<EcgDataPoint> dataPoints) {
        // Discretize amplitudes into bins
        int numBins = 20;
        double min = dataPoints.stream().mapToDouble(EcgDataPoint::amplitude).min().orElse(0.0);
        double max = dataPoints.stream().mapToDouble(EcgDataPoint::amplitude).max().orElse(1.0);
        double binWidth = (max - min) / numBins;
        
        if (binWidth == 0) return 0.0;
        
        int[] histogram = new int[numBins];
        for (EcgDataPoint point : dataPoints) {
            int bin = Math.min((int) ((point.amplitude() - min) / binWidth), numBins - 1);
            histogram[bin]++;
        }
        
        // Calculate entropy
        double entropy = 0.0;
        int total = dataPoints.size();
        for (int count : histogram) {
            if (count > 0) {
                double probability = count / (double) total;
                entropy -= probability * Math.log(probability);
            }
        }
        
        return entropy;
    }
}
