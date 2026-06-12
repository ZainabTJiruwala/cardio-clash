# ECG Anomaly Analyzer

A professional JavaFX desktop application for analyzing ECG (electrocardiogram) signals and detecting cardiac anomalies using machine learning and rule-based algorithms.

## Features

- 📁 **Load ECG Data**: Import CSV files from Kaggle datasets (MIT-BIH, PTB-XL, etc.)
- 📊 **Signal Visualization**: Interactive line chart displaying ECG waveforms with R-peak detection
- 🔍 **Anomaly Detection**: Identifies:
  - Tachycardia (high heart rate)
  - Bradycardia (low heart rate)
  - Arrhythmias (irregular rhythms)
  - Premature contractions
  - Myocardial Infarction (MI) - ML-based detection
  - Heart Murmurs - ML-based detection
- 📈 **Metrics Dashboard**: Real-time display of heart rate, RR intervals, and more
- 🎯 **Professional UI**: Clean, medical-grade interface with JavaFX
- 🌙 **Dark Mode**: Toggle between light and dark themes with persistent preferences
- 📄 **PDF Export**: Generate professional analysis reports with charts and metrics
- 📊 **Chart Export**: Save ECG charts as PNG or JPG images
- 🔬 **Advanced Signal Processing**: Industry-standard Pan-Tompkins algorithm for R-peak detection
- 🤖 **ML-Based Detection**: Neural network detection using DeepLearning4J for MI and heart murmurs

## Architecture

The application follows a **5-layer architecture**:

### 1. **Presentation Layer** (`view/`)
- JavaFX FXML-based UI
- Responsive layout with charts, tables, and controls
- Professional medical-grade design

### 2. **Controller Layer** (`controller/`)
- `EcgAnalyzerController`: Orchestrates UI interactions
- Handles file loading, analysis triggering, and results display

### 3. **Business Logic Layer** (`service/`)
- `AnalysisService`: Main analysis orchestrator
- `AnomalyDetector`: Interface for detection algorithms
  - `RuleBasedDetector`: Heart rate and rhythm analysis
  - `MLBasedDetector`: ML-based detection (placeholder)
- `RPeakDetector`: QRS complex detection

### 4. **Data Access Layer** (`repository/`)
- `EcgDataRepository`: Interface for data loading
- `CsvEcgDataRepository`: CSV file parser for Kaggle datasets

### 5. **Data Model Layer** (`model/`)
- `EcgDataPoint`: Individual ECG measurement
- `EcgSignal`: Complete ECG recording
- `Anomaly`: Detected cardiac anomaly
- `AnomalyType`: Enumeration of anomaly types

## Requirements

- **Java 17** or higher
- **Maven 3.6+** or **Gradle 7+**
- **JavaFX 21**

## Installation

### Option 1: Using IntelliJ IDEA (Recommended)

1. **Install IntelliJ IDEA** (Community or Ultimate)
2. **Import Project**:
   - Open IntelliJ IDEA
   - File → Open → Select `Java_project` folder
   - IntelliJ will auto-detect Maven/Gradle

3. **Run the Application**:
   - Open `EcgAnalyzerApp.java`
   - Click the green ▶️ Run button
   - Or right-click and select "Run 'EcgAnalyzerApp.main()'"

### Option 2: Command Line with Maven

```bash
# Navigate to project directory
cd c:\Users\taham\OneDrive\Java_project

# Run with Maven
mvn clean javafx:run

# Or build JAR
mvn clean package
java -jar target/ecg-analyzer-1.0.0.jar
```

## Usage

1. **Launch Application**
   - Run `EcgAnalyzerApp` from your IDE or command line

2. **Load ECG Data**
   - Click "📁 Load File" button
   - Select a CSV file with ECG data
   - Supported formats:
     - Two columns: time, amplitude
     - Two columns: index, amplitude (time calculated automatically)

3. **Configure Analysis** (Optional)
   - **Detection Algorithm**: Choose between Rule-Based or ML-Based (DL4J) detection
   - **Signal Processing**: Enable/disable filters:
     - Bandpass Filter (5-15 Hz) - Recommended for QRS enhancement
     - Baseline Wander Removal - Removes low-frequency drift
     - Noise Reduction - Reduces high-frequency noise

4. **Run Analysis**
   - Click "▶️ Run Analysis" button
   - Wait for processing to complete
   - Uses Pan-Tompkins algorithm for accurate R-peak detection

5. **Review Results**
   - View ECG waveform with detected R-peaks
   - Check metrics dashboard for heart rate and intervals
   - Review detected anomalies in the table
   - Double-click an anomaly to highlight its location

6. **Export Results**
   - **PDF Report**: Click "📄 Export PDF" to generate a comprehensive report with:
     - ECG chart snapshot
     - Metrics summary table
     - Detected anomalies table
     - Timestamp and metadata
   - **Chart Image**: Click "📊 Export Chart" to save as PNG or JPG

7. **Customize Appearance**
   - Click the theme toggle button (🌙/☀️) to switch between light and dark modes
   - Theme preference is saved automatically

## Sample Datasets

### Recommended Kaggle Datasets:

1. **MIT-BIH Arrhythmia Database**
   - URL: https://www.kaggle.com/datasets/shayanfazeli/heartbeat
   - Format: CSV with time/amplitude columns
   - Features: Multiple arrhythmia types

2. **PTB-XL ECG Database**
   - URL: https://www.kaggle.com/datasets/khyeh0719/ptb-xl-dataset
   - Format: CSV records
   - Features: Diagnostic labels including MI

3. **ECG Heartbeat Categorization**
   - URL: https://www.kaggle.com/datasets/shayanfazeli/heartbeat
   - Format: CSV arrays
   - Features: Normal and abnormal heartbeats

## Project Structure

```
Java_project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecg/analyzer/
│   │   │       ├── model/              # Data models (Records)
│   │   │       ├── repository/         # Data access layer
│   │   │       ├── service/            # Business logic
│   │   │       │   ├── detector/       # Anomaly detectors
│   │   │       │   └── preprocessing/  # Signal processing
│   │   │       ├── controller/         # JavaFX controllers
│   │   │       └── EcgAnalyzerApp.java # Main launcher
│   │   └── resources/
│   │       ├── view/                   # FXML files
│   │       ├── styles/                 # CSS stylesheets
│   │       └── application.properties  # Configuration
│   └── test/                           # Unit tests (future)
├── pom.xml                             # Maven configuration
└── README.md                           # This file
```

## Technologies Used

- **JavaFX 21**: UI framework
- **Apache Commons Math**: Signal processing
- **Apache Commons CSV**: CSV parsing
- **iText 5.5.13.3**: PDF report generation
- **DeepLearning4J 1.0.0-M2.1**: Machine learning framework
- **ND4J**: Backend for DeepLearning4J
- **SLF4J + Logback**: Logging
- **JUnit 5**: Testing (planned)

## Future Enhancements

- [x] Implement full Pan-Tompkins algorithm for R-peak detection
- [x] Add ML-based MI and heart murmur detection
- [x] Export analysis reports as PDF
- [x] Signal filtering (bandpass, baseline wander removal)
- [ ] Support for real-time ECG streaming
- [ ] Batch processing of multiple files
- [ ] Additional metrics (QT interval, ST segment analysis)
- [ ] Database integration for storing results
- [ ] Advanced chart features (zoom, pan)
- [ ] Model training interface for custom ML models

## Contributing

This is a personal project. Feel free to fork and modify for your own use.

## License

MIT License - Free to use and modify.

## Author

Built with ❤️ for cardiac health analysis

---

**Note**: This application is for research and educational purposes only. It is not intended for clinical diagnosis or medical decision-making.
