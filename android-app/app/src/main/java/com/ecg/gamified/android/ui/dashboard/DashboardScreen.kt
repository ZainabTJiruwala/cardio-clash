package com.ecg.gamified.android.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecg.gamified.android.ui.theme.MaroonPrimary
import com.ecg.gamified.android.ui.theme.glassmorphism

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "ECG Master",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaroonPrimary
        )

        // Mode Selection Cards
        GameModeCard(title = "Time Attack", description = "Race against the clock!")
        GameModeCard(title = "Case Study", description = "Diagnose patients with history.")
        GameModeCard(title = "Global Arena", description = "Compete on live leaderboards.")
    }
}

@Composable
fun GameModeCard(title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .glassmorphism(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaroonPrimary
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}
