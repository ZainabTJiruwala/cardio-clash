package com.ecg.analyzer.repository;

/**
 * Exception thrown when ECG data fails validation.
 */
public class DataValidationException extends Exception {
    
    public DataValidationException(String message) {
        super(message);
    }
    
    public DataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
