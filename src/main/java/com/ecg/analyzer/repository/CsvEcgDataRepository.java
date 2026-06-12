package com.ecg.analyzer.repository;

import com.ecg.analyzer.model.EcgDataPoint;
import com.ecg.analyzer.model.EcgSignal;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository implementation for loading ECG data from CSV files.
 * Supports common Kaggle ECG dataset formats.
 */
public class CsvEcgDataRepository implements EcgDataRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvEcgDataRepository.class);
    private static final int DEFAULT_SAMPLING_RATE = 360; // Common for MIT-BIH dataset
    
    @Override
    public EcgSignal loadFromFile(Path filePath) throws IOException, DataValidationException {
        logger.info("Loading ECG data from: {}", filePath);
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }
        
        String recordId = filePath.getFileName().toString().replaceFirst("[.][^.]+$", "");
        List<EcgDataPoint> dataPoints = new ArrayList<>();
        
        try (Reader reader = Files.newBufferedReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .build())) {
            
            int lineNumber = 0;
            for (CSVRecord record : csvParser) {
                lineNumber++;
                try {
                    EcgDataPoint dataPoint = parseRecord(record, lineNumber);
                    dataPoints.add(dataPoint);
                } catch (Exception e) {
                    logger.warn("Skipping invalid record at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }
        
        if (dataPoints.isEmpty()) {
            throw new DataValidationException("No valid data points found in file: " + filePath);
        }
        
        logger.info("Loaded {} data points from {}", dataPoints.size(), recordId);
        return new EcgSignal(recordId, DEFAULT_SAMPLING_RATE, dataPoints);
    }
    
    /**
     * Parses a CSV record into an EcgDataPoint.
     * Supports multiple common formats:
     * - Format 1: time, amplitude
     * - Format 2: index, amplitude (time calculated from index)
     * - Format 3: multiple leads (uses first lead)
     */
    private EcgDataPoint parseRecord(CSVRecord record, int lineNumber) throws DataValidationException {
        try {
            // Try to determine format based on header or column count
            double time;
            double amplitude;
            
            if (record.size() >= 2) {
                // Try first column as time/index
                String firstCol = record.get(0).trim();
                String secondCol = record.get(1).trim();
                
                double firstValue = Double.parseDouble(firstCol);
                amplitude = Double.parseDouble(secondCol);
                
                // If first value is very large (likely index), convert to time
                if (firstValue > 1000) {
                    time = firstValue / DEFAULT_SAMPLING_RATE;
                } else {
                    time = firstValue;
                }
                
                return new EcgDataPoint(time, amplitude);
            } else {
                throw new DataValidationException("Invalid record format at line " + lineNumber);
            }
        } catch (NumberFormatException e) {
            throw new DataValidationException("Invalid numeric value at line " + lineNumber, e);
        }
    }
    
    @Override
    public boolean supports(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".csv") || fileName.endsWith(".txt");
    }
    
    @Override
    public String getSupportedFormatsDescription() {
        return "CSV files (.csv, .txt) with time/index and amplitude columns";
    }
}
