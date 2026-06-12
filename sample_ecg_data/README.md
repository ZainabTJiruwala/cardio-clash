# Sample ECG Test Dataset

This directory contains synthetic ECG data files for testing the ECG Anomaly Analyzer application.

## Files

### 1. `bradycardia_45bpm.csv`
- **Condition**: Bradycardia (slow heart rate)
- **Heart Rate**: ~45 BPM (below normal threshold of 60 BPM)
- **Duration**: 10 seconds
- **Sampling Rate**: 360 Hz
- **Expected Detection**: Both Rule-Based and ML-Based detectors should identify bradycardia

### 2. `tachycardia_120bpm.csv`
- **Condition**: Tachycardia (fast heart rate)
- **Heart Rate**: ~120 BPM (above normal threshold of 100 BPM)
- **Duration**: 10 seconds
- **Sampling Rate**: 360 Hz
- **Expected Detection**: Both Rule-Based and ML-Based detectors should identify tachycardia

### 3. `mi_st_elevation.csv`
- **Condition**: Myocardial Infarction (ST segment elevation)
- **Heart Rate**: ~75 BPM (normal)
- **ST Elevation**: ~0.15 mV (elevated above baseline)
- **Duration**: 10 seconds
- **Sampling Rate**: 360 Hz
- **Expected Detection**: ML-Based detector should identify MI based on ST segment analysis

## File Format

All CSV files follow this format:
```
time,amplitude
0.0000,0.015234
0.0028,0.023145
...
```

- **time**: Time in seconds
- **amplitude**: ECG amplitude in millivolts (mV)

## Usage

1. Open the ECG Analyzer application
2. Click "Load File"
3. Navigate to this directory
4. Select one of the CSV files
5. Choose detection method (Rule-Based or ML-Based)
6. Click "Run Analysis"
7. Review detected anomalies in the results table

## Testing Tips

- **Bradycardia file**: Test both detectors to verify heart rate anomaly detection
- **Tachycardia file**: Verify that ML detector now detects tachycardia (previously didn't work)
- **MI file**: Test ML detector's ST segment elevation detection
- **Dark Mode**: Toggle the theme button (🌙/☀️) while viewing any file to test the new dark mode functionality

## Data Characteristics

- All files contain realistic ECG waveforms with:
  - P wave (atrial depolarization)
  - QRS complex (ventricular depolarization)
  - T wave (ventricular repolarization)
  - ST segment (between QRS and T wave)
  - Random noise for realism (~0.02 mV standard deviation)

- Total data points per file: ~3,600 points (10 seconds × 360 Hz)
