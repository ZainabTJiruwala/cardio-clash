package com.ecg.gamified.android.data

data class EcgSignal(
    val id: String,
    val data: List<Double>,
    val samplingRate: Double = 250.0
)
