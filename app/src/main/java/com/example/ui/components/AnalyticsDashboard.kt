package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPriority
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import com.example.ui.viewmodel.DayCompletion
import com.example.ui.viewmodel.TaskStats

@Composable
fun AnalyticsDashboard(
    stats: TaskStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Score & Completion Overview
        item {
            ProductivityHeaderCard(stats)
        }

        // Metrics 4-Grid Cards
        item {
            MetricsGrid(stats)
        }

        // Weekly Completion Chart
        item {
            WeeklyActivityCard(history = stats.weeklyHistory)
        }

        // Priority Breakdown
        item {
            PriorityBreakdownCard(priorityStats = stats.priorityBreakdown, total = stats.totalCount)
        }

        // Category Breakdown
        item {
            CategoryBreakdownCard(categoryStats = stats.categoryBreakdown, total = stats.totalCount)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProductivityHeaderCard(stats: TaskStats) {
    val animatedProgress by animateFloatAsState(
        targetValue = stats.completionRate,
        animationSpec = tween(durationMillis = 800),
        label = "prodRing"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Verimlilik Skoru",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.productivityScore} / 100",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (stats.productivityScore >= 80) "Harika! Görevlerinizi vaktinde tamamlıyorsunuz."
                    else if (stats.productivityScore >= 50) "İyi gidiyor! Bekleyen öncelikli işlere odaklanın."
                    else "Harekete geçin! Bekleyen görevler birikiyor.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Circular progress gauge
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF06B6D4), Color(0xFF6366F1))
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "%${(stats.completionRate * 100).toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MetricsGrid(stats: TaskStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            title = "Toplam",
            value = stats.totalCount.toString(),
            icon = Icons.Default.WorkHistory,
            iconTint = Color(0xFF6366F1)
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            title = "Tamamlanan",
            value = stats.completedCount.toString(),
            icon = Icons.Default.CheckCircle,
            iconTint = Color(0xFF10B981)
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            title = "Bekleyen",
            value = stats.pendingCount.toString(),
            icon = Icons.Default.PendingActions,
            iconTint = Color(0xFF3B82F6)
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            title = "Acil",
            value = stats.urgentPendingCount.toString(),
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFEF4444)
        )
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivityCard(history: List<DayCompletion>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Haftalık Tamamlama Trendi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Son 7 günde bitirilen görev sayıları",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = maxOf(history.maxOfOrNull { it.count } ?: 1, 4)

            // Custom Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val barWidth = 24.dp.toPx()
                val totalWidth = size.width
                val chartHeight = size.height - 24.dp.toPx()
                val spacing = (totalWidth - (barWidth * history.size)) / (history.size + 1)

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                history.forEachIndexed { index, item ->
                    val x = spacing + index * (barWidth + spacing)
                    val barHeight = if (maxVal > 0) (item.count.toFloat() / maxVal.toFloat()) * chartHeight else 0f
                    val y = chartHeight - barHeight

                    // Background track bar
                    drawRoundRect(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Active filled bar
                    if (barHeight > 4f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF06B6D4), Color(0xFF4F46E5)),
                                startY = y,
                                endY = chartHeight
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Count label on top of bar
                    if (item.count > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            item.count.toString(),
                            x + barWidth / 2f,
                            (y - 4.dp.toPx()).coerceAtLeast(10.dp.toPx()),
                            paint
                        )
                    }

                    // Day of week label
                    drawContext.canvas.nativeCanvas.drawText(
                        item.dayLabel,
                        x + barWidth / 2f,
                        size.height - 4.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityBreakdownCard(
    priorityStats: Map<TaskPriority, Int>,
    total: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Öncelik Dağılımı",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            TaskPriority.entries.forEach { prio ->
                val count = priorityStats[prio] ?: 0
                val ratio = if (total > 0) count.toFloat() / total.toFloat() else 0f
                val color = when (prio) {
                    TaskPriority.LOW -> PriorityLow
                    TaskPriority.MEDIUM -> PriorityMedium
                    TaskPriority.HIGH -> PriorityHigh
                    TaskPriority.URGENT -> PriorityUrgent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prio.displayName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(65.dp)
                    )

                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        text = "$count",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(35.dp)
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    categoryStats: Map<String, Int>,
    total: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Kategori Dağılımı",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (categoryStats.isEmpty()) {
                Text(
                    text = "Henüz kategori verisi bulunmuyor.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categoryStats.forEach { (catName, count) ->
                    val ratio = if (total > 0) count.toFloat() / total.toFloat() else 0f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = catName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(80.dp)
                        )

                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            text = "$count",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .width(35.dp)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
