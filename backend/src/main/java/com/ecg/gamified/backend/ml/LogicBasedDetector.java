package com.ecg.gamified.backend.ml;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogicBasedDetector {

    private final FeatureExtractor featureExtractor;

    public List<String> detectAnomalies(EcgSignal signal, List<Integer> rPeakIndices) {
        log.info("Running detection for signal {}", signal.getId());
        List<String> anomalies = new ArrayList<>();
        
        try {
            double[] features = featureExtractor.extractFeatures(signal, rPeakIndices);
            
            anomalies.addAll(detectTachycardia(features));
            anomalies.addAll(detectBradycardia(features));
            anomalies.addAll(detectMI(features));
            anomalies.addAll(detectMurmur(features));
            
        } catch (Exception e) {
            log.error("Detection failed", e);
        }
        
        return anomalies;
    }

    private List<String> detectTachycardia(double[] features) {
        List<String> results = new ArrayList<>();
        if (features.length > 9) {
            double heartRate = features[9];
            if (heartRate > 100.0) {
                results.add("Tachycardia detected: " + String.format("%.1f", heartRate) + " BPM");
            }
        }
        return results;
    }

    private List<String> detectBradycardia(double[] features) {
        List<String> results = new ArrayList<>();
        if (features.length > 9) {
            double heartRate = features[9];
            if (heartRate < 60.0 && heartRate > 0) {
                results.add("Bradycardia detected: " + String.format("%.1f", heartRate) + " BPM");
            }
        }
        return results;
    }

    private List<String> detectMI(double[] features) {
        List<String> results = new ArrayList<>();
        if (features.length > 13) {
            double stElevationAbs = features[13];
            double stElevation = features[11];
            
            if (stElevationAbs > 0.05) {
                String type = stElevation > 0 ? "ST-Elevation" : "ST-Depression";
                results.add("Myocardial Infarction marker: " + type + " (" + String.format("%.3f", stElevation) + " mV)");
            }
        }
        return results;
    }

    private List<String> detectMurmur(double[] features) {
         List<String> results = new ArrayList<>();
         if (features.length > 18) {
             double zeroCrossingRate = features[16];
             double entropy = features[18];
             
             if (entropy > 1.8 && zeroCrossingRate > 8) {
                 results.add("Possible Heart Murmur (Irregular noise detected)");
             }
         }
         return results;
    }
}
