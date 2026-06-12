package com.ecg.gamified.backend.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts features from ECG signals for machine learning models.
 * Ported from original desktop application.
 */
@Service
@Slf4j
public class FeatureExtractor {
    
    /**
     * Extracts a comprehensive feature vector from an ECG signal and R-peak locations.
     */
    public double[] extractFeatures(EcgSignal signal, List<Integer> rPeakIndices) {
        log.debug("Extracting ML features from signal {}", signal.getId());
        
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
        log.debug("Extracted {} features", featureArray.length);
        
        return featureArray;
    }
    
    private List<Double> extractStatisticalFeatures(EcgSignal signal) {
        List<Double> features = new ArrayList<>();
        double[] data = signal.getData();
        
        if (data.length == 0) {
            return List.of(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        // Mean amplitude
        double sum = 0;
        for (double d : data) sum += d;
        double mean = sum / data.length;
        features.add(mean);
        
        // Standard deviation
        double sumSqDiff = 0;
        for (double d : data) sumSqDiff += Math.pow(d - mean, 2);
        double variance = sumSqDiff / data.length;
        features.add(Math.sqrt(variance));
        
        // Min and Max
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double d : data) {
            if (d < min) min = d;
            if (d > max) max = d;
        }
        features.add(min);
        features.add(max);
        features.add(max - min); // Range
        
        return features;
    }
    
    private List<Double> extractRPeakFeatures(EcgSignal signal, List<Integer> rPeakIndices) {
        List<Double> features = new ArrayList<>();
        
        if (rPeakIndices.size() < 2) {
            // Not enough R-peaks, add zeros
            for (int i = 0; i < 5; i++) features.add(0.0);
            return features;
        }
        
        double[] data = signal.getData();
        double samplingRate = signal.getSamplingRate();
        
        // Average R-peak amplitude
        double avgRPeakAmplitude = rPeakIndices.stream()
            .mapToDouble(idx -> idx < data.length ? data[idx] : 0.0)
            .average()
            .orElse(0.0);
        features.add(avgRPeakAmplitude);
        
        // RR intervals (in seconds)
        List<Double> rrIntervals = new ArrayList<>();
        for (int i = 1; i < rPeakIndices.size(); i++) {
            double rrInterval = (rPeakIndices.get(i) - rPeakIndices.get(i - 1)) / samplingRate;
            rrIntervals.add(rrInterval);
        }
        
        // Mean RR interval
        double meanRR = rrIntervals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        features.add(meanRR);
        
        // SDNN (Standard Deviation of RR intervals)
        double rrVariance = rrIntervals.stream()
            .mapToDouble(rr -> Math.pow(rr - meanRR, 2))
            .average()
            .orElse(0.0);
        features.add(Math.sqrt(rrVariance));
        
        // RMSSD
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
        double heartRate = meanRR > 0 ? 60.0 / meanRR : 0.0;
        features.add(heartRate);
        
        return features;
    }
    
    private List<Double> extractMorphologicalFeatures(EcgSignal signal, List<Integer> rPeakIndices) {
        List<Double> features = new ArrayList<>();
        
        if (rPeakIndices.isEmpty()) {
            for (int i = 0; i < 6; i++) features.add(0.0);
            return features;
        }
        
        double[] data = signal.getData();
        double samplingRate = signal.getSamplingRate();
        
        int beatsToAnalyze = Math.min(5, rPeakIndices.size());
        
        double avgQrsWidth = 0.0;
        double avgStElevation = 0.0;
        double avgTWaveAmplitude = 0.0;
        
        for (int i = 0; i < beatsToAnalyze && i < rPeakIndices.size() - 1; i++) {
            int rPeakIdx = rPeakIndices.get(i);
            if (rPeakIdx >= data.length) continue;
            
            // QRS width
            int qrsStart = Math.max(0, rPeakIdx - (int)(0.06 * samplingRate));
            int qrsEnd = Math.min(data.length - 1, rPeakIdx + (int)(0.06 * samplingRate));
            avgQrsWidth += (qrsEnd - qrsStart) / samplingRate;
            
            // Baseline
            int prStart = Math.max(0, rPeakIdx - (int)(0.2 * samplingRate));
            int prEnd = Math.max(0, rPeakIdx - (int)(0.08 * samplingRate));
            double baseline = 0.0;
            if (prEnd > prStart) {
                for (int j = prStart; j < prEnd && j < data.length; j++) {
                    baseline += data[j];
                }
                baseline /= (prEnd - prStart);
            }
            
            // ST segment
            int stSegmentIdx = Math.min(data.length - 1, rPeakIdx + (int)(0.08 * samplingRate));
            if (stSegmentIdx < data.length) {
                avgStElevation += (data[stSegmentIdx] - baseline);
            }
            
            // T-wave
            int tWaveIdx = Math.min(data.length - 1, rPeakIdx + (int)(0.3 * samplingRate));
            if (tWaveIdx < data.length) {
                avgTWaveAmplitude += data[tWaveIdx];
            }
        }
        
        avgQrsWidth /= beatsToAnalyze;
        avgStElevation /= beatsToAnalyze;
        avgTWaveAmplitude /= beatsToAnalyze;
        
        features.add(avgQrsWidth);
        features.add(avgStElevation);
        features.add(avgTWaveAmplitude);
        features.add(Math.abs(avgStElevation));
        
        // QRS amplitude
        double avgQrsAmplitude = rPeakIndices.stream()
            .mapToDouble(idx -> idx < data.length ? data[idx] : 0.0)
            .average()
            .orElse(0.0);
        features.add(avgQrsAmplitude);
        
        // T/R ratio
        double trRatio = avgQrsAmplitude != 0 ? avgTWaveAmplitude / avgQrsAmplitude : 0.0;
        features.add(trRatio);
        
        return features;
    }
    
    private List<Double> extractFrequencyFeatures(EcgSignal signal) {
        List<Double> features = new ArrayList<>();
        double[] data = signal.getData();
        
        if (data.length == 0) return List.of(0.0, 0.0, 0.0);

        int zeroCrossings = 0;
        double sum = 0;
        for (double d : data) sum += d;
        double mean = sum / data.length;
        
        for (int i = 1; i < data.length; i++) {
            if ((data[i] - mean) * (data[i - 1] - mean) < 0) {
                zeroCrossings++;
            }
        }
        
        double duration = signal.duration();
        double crossingRate = duration > 0 ? zeroCrossings / duration : 0.0;
        features.add(crossingRate);
        
        double energy = 0;
        for (double d : data) energy += d * d;
        features.add(energy);
        
        double entropy = calculateSimpleEntropy(data);
        features.add(entropy);
        
        return features;
    }
    
    private double calculateSimpleEntropy(double[] data) {
        int numBins = 20;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        
        for (double d : data) {
            if (d < min) min = d;
            if (d > max) max = d;
        }
        
        double binWidth = (max - min) / numBins;
        if (binWidth == 0) return 0.0;
        
        int[] histogram = new int[numBins];
        for (double d : data) {
            int bin = Math.min((int) ((d - min) / binWidth), numBins - 1);
            if (bin >= 0 && bin < numBins) {
                histogram[bin]++;
            }
        }
        
        double entropy = 0.0;
        int total = data.length;
        for (int count : histogram) {
            if (count > 0) {
                double probability = count / (double) total;
                entropy -= probability * Math.log(probability);
            }
        }
        
        return entropy;
    }
}
