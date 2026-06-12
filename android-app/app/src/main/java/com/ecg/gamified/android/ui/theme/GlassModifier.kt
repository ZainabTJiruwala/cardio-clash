package com.ecg.gamified.android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Custom modifier to apply Glassmorphism effect
fun Modifier.glassmorphism(
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 10.dp
): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFF).copy(alpha = 0.6f),
                Color(0xFFFFFFF).copy(alpha = 0.2f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = 1.dp,
        color = Color(0xFFFFFFF).copy(alpha = 0.3f),
        shape = RoundedCornerShape(cornerRadius)
    )
    // Blur requires API 31+, fallback handled by system or ignored
    .blur(radius = blurRadius)
