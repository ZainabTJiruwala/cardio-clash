package com.ecg.gamified.android.network

import com.ecg.gamified.android.data.EcgSignal
import retrofit2.http.Body
import retrofit2.http.POST

interface GameApiService {
    @POST("api/game/analyze")
    suspend fun analyzeSignal(@Body signal: EcgSignal): GameResponse
}

data class GameResponse(
    val anomalies: List<String>,
    val score: Int
)
