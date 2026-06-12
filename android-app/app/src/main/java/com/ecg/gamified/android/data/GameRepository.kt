package com.ecg.gamified.android.data

import com.ecg.gamified.android.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository {
    private val api = RetrofitClient.apiService

    suspend fun analyzeSignal(signalData: List<Double>): Pair<List<String>, Int> {
        return withContext(Dispatchers.IO) {
            try {
                // Create a random ID for the session
                val signal = EcgSignal(
                    id = "signal_${System.currentTimeMillis()}",
                    data = signalData
                )
                
                val response = api.analyzeSignal(signal)
                Pair(response.anomalies, response.score)
            } catch (e: Exception) {
                // Fallback for offline testing or errors
                e.printStackTrace()
                Pair(listOf("Error: ${e.message}"), 0)
            }
        }
    }
}
