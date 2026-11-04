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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
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

    // Legacy or corrupt stored values must not produce invalid bars or averages.
    val total = stats.totalNotifications.coerceAtLeast(0)
    val tapped = stats.totalTapped.coerceAtLeast(0)
    val dismissed = stats.totalDismissed.coerceAtLeast(0)
    val engagement = DashboardMetricFormatter.percentage(tapped, total)
    val dismissal = DashboardMetricFormatter.percentage(dismissed, total)
    val days = remember(stats.firstUseTimestamp) {
        TimeUnit.MILLISECONDS
            .toDays(System.currentTimeMillis() - stats.firstUseTimestamp)
            .coerceAtLeast(1)
    }
    val resetContentDescription = stringResource(R.string.dashboard_reset_content_description)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DashboardCard {
            DashboardTitle(R.string.dashboard_overview)
            StatRow(Icons.Rounded.Notifications, MaterialTheme.colorScheme.primary, stringResource(R.string.dashboard_total_notifications), total.toString())
            StatRow(Icons.Rounded.TouchApp, Color(0xFF10B981), stringResource(R.string.dashboard_opened_tapped), tapped.toString())
            StatRow(Icons.Rounded.Delete, Color(0xFFEF4444), stringResource(R.string.dashboard_dismissed), dismissed.toString())
            Spacer(modifier = Modifier.height(4.dp))
            RateBar(stringResource(R.string.dashboard_engagement_rate), engagement, MaterialTheme.colorScheme.primary)
            RateBar(stringResource(R.string.dashboard_dismissal_rate), dismissal, MaterialTheme.colorScheme.error)
        }

        DashboardCard {
            DashboardTitle(R.string.dashboard_top_apps)
            if (stats.topApps.isEmpty()) {
                EmptyAppsState()
            } else {
                val maximum = stats.topApps.maxOf { it.count.coerceAtLeast(0) }
                stats.topApps.forEachIndexed { index, app ->
                    AppStatRow(stat = app, maximum = maximum, rank = index + 1)
                }
            }
        }

        DashboardCard {
            DashboardTitle(R.string.dashboard_insights)
            StatRow(Icons.Rounded.Analytics, Color(0xFF8B5CF6), stringResource(R.string.dashboard_days_active), days.toString())
            StatRow(Icons.Rounded.Bolt, Color(0xFFF59E0B), stringResource(R.string.dashboard_average_notifications_per_day), (total / days).toString())
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = resetContentDescription },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            onClick = { confirmReset = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = .8f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dashboard_reset_statistics),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error.copy(alpha = .8f)
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(text = stringResource(R.string.dashboard_reset_confirmation_title)) },
            text = { Text(text = stringResource(R.string.dashboard_reset_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch { statsRepository.resetStats() }
                }) {
                    Text(text = stringResource(R.string.dashboard_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(text = stringResource(R.string.dashboard_cancel))
                }
            }
        )
    }
}

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun DashboardTitle(stringRes: Int) {
    Text(
        text = stringResource(stringRes),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = .8.sp
    )
}

@Composable
private fun RateBar(label: String, percentage: Int, color: Color) {
    val safePercentage = percentage.coerceIn(0, 100)
    val progressDescription = stringResource(
        R.string.dashboard_rate_progress_description,
        label,
        safePercentage
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.dashboard_percentage, safePercentage),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))
                .semantics { contentDescription = progressDescription }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safePercentage / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun EmptyAppsState() {
    val noActivityContentDescription = stringResource(R.string.dashboard_no_app_activity_content_description)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = noActivityContentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Analytics,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(text = stringResource(R.string.dashboard_no_activity_yet), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = stringResource(R.string.dashboard_no_activity_message), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, color: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppStatRow(stat: AppStat, maximum: Int, rank: Int) {
    val count = stat.count.coerceAtLeast(0)
    val fraction = if (maximum > 0) (count.toFloat() / maximum).coerceIn(0f, 1f) else 0f
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val name = stat.appName.ifBlank { stat.packageName }.ifBlank {
        stringResource(R.string.dashboard_unknown_app)
    }
    val rankDescription = stringResource(R.string.dashboard_app_rank_description, rank, name, count)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = .2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = rank.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = rankColor)
                }
                Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            Text(text = count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f))
                .semantics { contentDescription = rankDescription }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .6f))
            )
        }
    }
}
