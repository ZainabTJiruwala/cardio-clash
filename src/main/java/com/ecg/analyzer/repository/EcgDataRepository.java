package com.ecg.analyzer.repository;

import com.ecg.analyzer.model.EcgSignal;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Repository interface for loading ECG data from various sources.
 * This abstraction allows for different data sources (CSV, Database, API, etc.)
 */
public interface EcgDataRepository {
    
    /**
     * Loads ECG signal data from a file.
     * 
     * @param filePath Path to the ECG data file
     * @return EcgSignal containing the loaded data
     * @throws IOException if file cannot be read
     * @throws DataValidationException if data format is invalid
     */
    EcgSignal loadFromFile(Path filePath) throws IOException, DataValidationException;
    
    /**
     * Validates if the file format is supported.
     * 
     * @param filePath Path to check
     * @return true if the file can be loaded by this repository
     */
    boolean supports(Path filePath);
    
    /**
     * Gets a human-readable description of supported formats.
     */
    String getSupportedFormatsDescription();
}
