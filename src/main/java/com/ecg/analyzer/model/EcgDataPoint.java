package com.ecg.analyzer.model;

/**
 * Represents a single ECG data point with timestamp and amplitude.
 * Using Java Record for immutability and automatic equals/hashCode/toString.
 */
public record EcgDataPoint(
    double time,      // Time in seconds
    double amplitude  // Amplitude in millivolts (mV)
) {
    /**
     * Validates that the data point has valid values.
     */
    public EcgDataPoint {
        if (time < 0) {
            throw new IllegalArgumentException("Time cannot be negative: " + time);
        }
    }
    
    @Override
    public String toString() {
        return String.format("EcgDataPoint[time=%.3fs, amplitude=%.3fmV]", time, amplitude);
    }
}
