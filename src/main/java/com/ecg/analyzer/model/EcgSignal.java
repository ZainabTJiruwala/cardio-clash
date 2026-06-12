package com.ecg.analyzer.model;

import java.util.List;

/**
 * Represents a complete ECG signal composed of multiple data points.
 */
public record EcgSignal(
    String recordId,
    int samplingRate,           // Samples per second (Hz)
    List<EcgDataPoint> dataPoints
) {
    /**
     * Validates the ECG signal.
     */
    public EcgSignal {
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("Record ID cannot be null or empty");
        }
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive: " + samplingRate);
        }
        if (dataPoints == null || dataPoints.isEmpty()) {
            throw new IllegalArgumentException("Data points cannot be null or empty");
        }
        // Make defensive copy to ensure immutability
        dataPoints = List.copyOf(dataPoints);
    }
    
    /**
     * Gets the duration of the signal in seconds.
     */
    public double getDuration() {
        if (dataPoints.isEmpty()) return 0.0;
        return dataPoints.get(dataPoints.size() - 1).time();
    }
    
    /**
     * Gets the duration of the signal in seconds (record-style accessor).
     */
    public double duration() {
        return getDuration();
    }
    
    /**
     * Gets the number of data points.
     */
    public int size() {
        return dataPoints.size();
    }
    
    @Override
    public String toString() {
        return String.format("EcgSignal[id=%s, rate=%dHz, points=%d, duration=%.2fs]", 
            recordId, samplingRate, size(), getDuration());
    }
}
