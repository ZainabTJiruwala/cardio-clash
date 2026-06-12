package com.ecg.gamified.android.data

object SampleData {
    fun generateSinWave(): List<Float> {
        val points = mutableListOf<Float>()
        for (i in 0..500) {
            // Simulate P-QRS-T complex roughly with sin waves
            val base = Math.sin(i * 0.1).toFloat()
            var value = base
            
            // Artificial R-peak
            if (i % 100 in 45..55) {
                value += 5f * (if (i % 100 == 50) 1f else 0.5f)
            }
            
            points.add(value)
        }
        return points
    }
}
