package com.ecg.gamified.android.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecg.gamified.android.data.SampleData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _ecgData = MutableStateFlow<List<Float>>(emptyList())
    val ecgData = _ecgData.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    private val _timeLeft = MutableStateFlow(60) // 60 seconds for Time Attack
    val timeLeft = _timeLeft.asStateFlow()

    init {
        startGame()
    }

    private fun startGame() {
        // Load initial data
        _ecgData.value = SampleData.generateSinWave()

        // Start timer
        viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000)
                _timeLeft.value -= 1
            }
        }

        // Simulate real-time data streaming (scrolling effect)
        viewModelScope.launch {
            while (true) {
                delay(100) // Update every 100ms
                // In a real app, we'd append new points and remove old ones
                // For now, we just rotate the static mock data to simulate movement
                val current = _ecgData.value.toMutableList()
                if (current.isNotEmpty()) {
                    val first = current.removeAt(0)
                    current.add(first)
                    _ecgData.value = current
                }
            }
        }
    }
    
    private val repository = GameRepository()
    
    // ... (rest of simple mock data logic for chart remains for now)

    fun submitDiagnosis(anomaly: String) {
        viewModelScope.launch {
            // In a real game, we would send the current window of data
            val currentData = _ecgData.value.map { it.toDouble() }
            val (findings, backendScore) = repository.analyzeSignal(currentData)
            
            // Check if player's guess matches backend findings
            val isCorrect = findings.any { it.contains(anomaly, ignoreCase = true) }
            
            if (isCorrect) {
                _score.value += 100 + (backendScore / 10) // Bonus from backend
            } else if (anomaly == "Normal" && findings.isEmpty()) {
                _score.value += 50
            } else {
               // Penalty
               _score.value = (_score.value - 20).coerceAtLeast(0)
            }
        }
    }
}
