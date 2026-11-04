/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */
package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.AppStat
import com.agupta07505.smartisland.data.NotificationStats
import com.agupta07505.smartisland.data.NotificationStatsRepository
import com.agupta07505.smartisland.ui.DashboardMetricFormatter
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsSection(statsRepository: NotificationStatsRepository) {
    val stats by statsRepository.stats.collectAsState(initial = NotificationStats())
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    // Display only usable counts: legacy/corrupted storage cannot produce invalid bars or averages.
    val total = stats.totalNotifications.coerceAtLeast(0)
    val tapped = stats.totalTapped.coerceAtLeast(0)
    val dismissed = stats.totalDismissed.coerceAtLeast(0)
    val engagement = DashboardMetricFormatter.percentage(tapped, total)
    val dismissal = DashboardMetricFormatter.percentage(dismissed, total)
    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DashboardCard {
            Title("Overview")
            StatRow(Icons.Rounded.Notifications, MaterialTheme.colorScheme.primary, "Total Notifications", total.toString())
            StatRow(Icons.Rounded.TouchApp, Color(0xFF10B981), "Opened / Tapped", tapped.toString())
            StatRow(Icons.Rounded.Delete, Color(0xFFEF4444), "Dismissed", dismissed.toString())
            Spacer(Modifier.height(4.dp))
            RateBar("Engagement Rate", engagement, MaterialTheme.colorScheme.primary)
            RateBar("Dismissal Rate", dismissal, MaterialTheme.colorScheme.error)
        }
        DashboardCard {
            Title("Top Apps")
            if (stats.topApps.isEmpty()) EmptyAppsState() else {
                val maximum = stats.topApps.maxOf { it.count.coerceAtLeast(0) }
                stats.topApps.forEachIndexed { index, app -> AppStatRow(app, maximum, index + 1) }
            }
        }
        val days = remember(stats.firstUseTimestamp) { TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - stats.firstUseTimestamp).coerceAtLeast(1) }
        DashboardCard {
            Title("Insights")
            StatRow(Icons.Rounded.Analytics, Color(0xFF8B5CF6), "Days Active", days.toString())
            StatRow(Icons.Rounded.Bolt, Color(0xFFF59E0B), "Avg. Notifications / Day", (total / days).toString())
        }
        Card(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Reset all notification statistics" },
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), onClick = { confirmReset = true }
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Refresh, null, tint = MaterialTheme.colorScheme.error.copy(alpha = .8f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset Statistics", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error.copy(alpha = .8f))
            }
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false }, title = { Text("Reset statistics?") },
        text = { Text("This permanently clears notification totals and the app ranking on this device.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; scope.launch { statsRepository.resetStats() } }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
    )
}

@Composable private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) = Card(
    Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)),
    CardDefaults.cardElevation(defaultElevation = 0.dp)
) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }

@Composable private fun Title(text: String) = Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = .8.sp)

@Composable private fun RateBar(label: String, percentage: Int, color: Color) {
    val safe = percentage.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text("$safe%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)).semantics { contentDescription = "$label: $safe percent" }) {
            Box(Modifier.fillMaxWidth(safe / 100f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable private fun EmptyAppsState() = Row(Modifier.fillMaxWidth().semantics { contentDescription = "No app activity recorded yet" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Icon(Icons.Rounded.Analytics, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    Column {
        Text("No activity yet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text("Apps will appear here after their notifications are received.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun StatRow(icon: ImageVector, color: Color, label: String, value: String) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }; Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable private fun AppStatRow(stat: AppStat, maximum: Int, rank: Int) {
    val count = stat.count.coerceAtLeast(0); val fraction = if (maximum > 0) (count.toFloat() / maximum).coerceIn(0f, 1f) else 0f
    val rankColor = when (rank) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val name = stat.appName.ifBlank { stat.packageName }.ifBlank { "Unknown app" }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(rankColor.copy(alpha = .2f)), contentAlignment = Alignment.Center) { Text("$rank", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = rankColor) }
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            }; Text(count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f))) { Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .6f))) }
    }
}
