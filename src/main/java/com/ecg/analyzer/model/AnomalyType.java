package com.ecg.analyzer.model;

/**
 * Enumeration of detectable ECG anomaly types.
 */
public enum AnomalyType {
    TACHYCARDIA("Tachycardia", "Heart rate > 100 BPM"),
    BRADYCARDIA("Bradycardia", "Heart rate < 60 BPM"),
    MYOCARDIAL_INFARCTION("Myocardial Infarction", "ST segment elevation/depression"),
    HEART_MURMUR("Heart Murmur", "Abnormal heart sounds/rhythm"),
    ARRHYTHMIA("Arrhythmia", "Irregular heart rhythm"),
    PREMATURE_CONTRACTION("Premature Contraction", "Early heartbeat"),
    ATRIAL_FIBRILLATION("Atrial Fibrillation", "Irregular atrial activity");
    
    private final String displayName;
    private final String description;
    
    AnomalyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
