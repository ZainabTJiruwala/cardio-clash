package com.ecg.gamified.backend.ml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EcgSignal {
    private String id;
    private double[] data;
    private double samplingRate;

    public double duration() {
        return data.length / samplingRate;
    }
}
