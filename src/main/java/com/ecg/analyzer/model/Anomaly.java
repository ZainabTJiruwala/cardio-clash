package com.ecg.analyzer.model;

/**
 * Represents a detected anomaly in the ECG signal.
 */
public record Anomaly(
    AnomalyType type,
    double timestamp,        // When the anomaly occurred (seconds)
    Severity severity,
    double confidence,       // Confidence score (0.0 to 1.0)
    String details          // Additional information
) {
    /**
     * Severity levels for anomalies.
     */
    public enum Severity {
        LOW("🟢", "Low"),
        MEDIUM("🟡", "Medium"),
        HIGH("🔴", "High");
        
        private final String icon;
        private final String label;
        
        Severity(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }
        
        public String getIcon() {
            return icon;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    /**
     * Validates the anomaly.
     */
    public Anomaly {
        if (type == null) {
            throw new IllegalArgumentException("Anomaly type cannot be null");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Gets confidence as a percentage.
     */
    public int getConfidencePercent() {
        return (int) (confidence * 100);
    }
    
    @Override
    public String toString() {
        return String.format("%s at %.2fs [%s, %d%% confidence]: %s",
            type.getDisplayName(), timestamp, severity.getLabel(), 
            getConfidencePercent(), details);
    }
}
