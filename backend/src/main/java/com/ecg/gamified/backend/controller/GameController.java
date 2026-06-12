package com.ecg.gamified.backend.controller;

import com.ecg.gamified.backend.ml.EcgSignal;
import com.ecg.gamified.backend.ml.LogicBasedDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final LogicBasedDetector detector;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeSignal(@RequestBody EcgSignal signal, @RequestParam(required = false) List<Integer> rPeaks) {
        // In a real app, R-peaks might be detected here if not sent by client
        // For now we assume client sends them or we mock them (simplified)
        if (signal.getSamplingRate() == 0.0) {
            signal.setSamplingRate(250.0);
        }

        if (rPeaks == null || rPeaks.isEmpty()) {
            rPeaks = new java.util.ArrayList<>(); 
            // Simple peak detection: find values > 3.0 (which correspond to R-peaks in our generated signal)
            double[] data = signal.getData();
            for (int i = 1; i < data.length; i++) {
                if (data[i] > 3.0 && data[i-1] <= 3.0) {
                    rPeaks.add(i);
                }
            }
        }
        
        List<String> findings = detector.detectAnomalies(signal, rPeaks);
        
        Map<String, Object> response = new HashMap<>();
        response.put("anomalies", findings);
        response.put("score", calculateScore(findings));
        
        return ResponseEntity.ok(response);
    }
    
    private int calculateScore(List<String> findings) {
        // Simple scoring logic: 100 points per correct finding (simulated)
        // In real game, we'd compare against ground truth
        return findings.isEmpty() ? 0 : findings.size() * 100;
    }
}
