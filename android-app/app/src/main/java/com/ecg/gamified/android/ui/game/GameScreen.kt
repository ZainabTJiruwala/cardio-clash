package com.ecg.gamified.android.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecg.gamified.android.ui.graph.EcgChart
import com.ecg.gamified.android.ui.theme.MaroonPrimary
import com.ecg.gamified.android.ui.theme.RedAlert
import com.ecg.gamified.android.ui.theme.glassmorphism

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val ecgData by viewModel.ecgData.collectAsState()
    val score by viewModel.score.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HUDItem(label = "SCORE", value = score.toString())
            HUDItem(label = "TIME", value = "${timeLeft}s", color = if (timeLeft < 10) RedAlert else Color.Black)
        }

        // ECG Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
                .glassmorphism()
        ) {
            EcgChart(dataPoints = ecgData, modifier = Modifier.fillMaxSize())
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DiagnosisButton("Normal", Color(0xFF4CAF50)) { viewModel.submitDiagnosis("Normal") }
            DiagnosisButton("Tachycardia", RedAlert) { viewModel.submitDiagnosis("Tachycardia") }
            DiagnosisButton("Bradycardia", Color.Blue) { viewModel.submitDiagnosis("Bradycardia") }
        }
    }
}

@Composable
fun HUDItem(label: String, value: String, color: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DiagnosisButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(text = text)
    }
}
