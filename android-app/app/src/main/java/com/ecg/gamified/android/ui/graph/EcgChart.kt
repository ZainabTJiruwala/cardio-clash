package com.ecg.gamified.android.ui.graph

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun EcgChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                
                // Style the grid
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(true)
                xAxis.gridColor = Color.parseColor("#40800000") // Translucent Maroon
                
                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor = Color.parseColor("#40800000")
                axisRight.isEnabled = false
                
                legend.isEnabled = false
                
                // Background color (transparent to let Glassmorphism show, or specific beige)
                setBackgroundColor(Color.TRANSPARENT)
            }
        },
        update = { chart ->
            val entries = dataPoints.mapIndexed { index, value ->
                Entry(index.toFloat(), value)
            }
            
            val dataSet = LineDataSet(entries, "ECG Signal").apply {
                color = Color.RED
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(false)
            }
            
            chart.data = LineData(dataSet)
            chart.invalidate() // Refresh
        }
    )
}
