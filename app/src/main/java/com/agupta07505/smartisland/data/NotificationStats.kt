/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tracks notification statistics per app for the Statistics & Insights dashboard.
 * Stored in a separate DataStore file to avoid bloating the main settings.
 */
class NotificationStatsRepository(private val context: Context) {

    private val statsFlow: Flow<NotificationStats> = context.smartIslandStatsDataStore.data
        .map { prefs ->
            val totalNotifications = prefs[Keys.TotalNotifications] ?: 0
            val totalDismissed = prefs[Keys.TotalDismissed] ?: 0
            val totalTapped = prefs[Keys.TotalTapped] ?: 0
            val perAppJson = prefs[Keys.PerAppStats] ?: "{}"
            val perApp = parsePerAppStats(perAppJson)
            val firstUseTimestamp = prefs[Keys.FirstUseTimestamp] ?: System.currentTimeMillis()
            NotificationStats(
                totalNotifications = totalNotifications,
                totalDismissed = totalDismissed,
                totalTapped = totalTapped,
                perAppStats = perApp,
                firstUseTimestamp = firstUseTimestamp
            )
        }

    val stats: Flow<NotificationStats> = statsFlow

    suspend fun recordNotification(appName: String, packageName: String, mode: String) {
        context.smartIslandStatsDataStore.edit { prefs ->
            val currentTotal = prefs[Keys.TotalNotifications] ?: 0
            prefs[Keys.TotalNotifications] = currentTotal + 1
            if (prefs[Keys.FirstUseTimestamp] == null) {
                prefs[Keys.FirstUseTimestamp] = System.currentTimeMillis()
            }
            val perAppJson = prefs[Keys.PerAppStats] ?: "{}"
            val perApp = parsePerAppStats(perAppJson).toMutableMap()
            val key = "$packageName|$appName"
            val existing = perApp[key] ?: AppStat(packageName = packageName, appName = appName)
            perApp[key] = existing.copy(count = existing.count + 1, lastSeen = System.currentTimeMillis())
            prefs[Keys.PerAppStats] = serializePerAppStats(perApp)
        }
    }

    suspend fun recordDismiss() {
        context.smartIslandStatsDataStore.edit { prefs ->
            prefs[Keys.TotalDismissed] = (prefs[Keys.TotalDismissed] ?: 0) + 1
        }
    }

    suspend fun recordTap() {
        context.smartIslandStatsDataStore.edit { prefs ->
            prefs[Keys.TotalTapped] = (prefs[Keys.TotalTapped] ?: 0) + 1
        }
    }

    suspend fun resetStats() {
        context.smartIslandStatsDataStore.edit { it.clear() }
    }

    private fun parsePerAppStats(json: String): Map<String, AppStat> {
        return runCatching {
            val root = JSONObject(json)
            val result = mutableMapOf<String, AppStat>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.optJSONObject(key) ?: continue
                result[key] = AppStat(
                    packageName = obj.optString("packageName"),
                    appName = obj.optString("appName"),
                    count = obj.optInt("count"),
                    lastSeen = obj.optLong("lastSeen")
                )
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun serializePerAppStats(stats: Map<String, AppStat>): String {
        val root = JSONObject()
        stats.forEach { (key, stat) ->
            val obj = JSONObject()
            obj.put("packageName", stat.packageName)
            obj.put("appName", stat.appName)
            obj.put("count", stat.count)
            obj.put("lastSeen", stat.lastSeen)
            root.put(key, obj)
        }
        return root.toString()
    }

    private object Keys {
        val TotalNotifications = intPreferencesKey("total_notifications")
        val TotalDismissed = intPreferencesKey("total_dismissed")
        val TotalTapped = intPreferencesKey("total_tapped")
        val PerAppStats = stringPreferencesKey("per_app_stats")
        val FirstUseTimestamp = longPreferencesKey("first_use_timestamp")
    }
}

data class NotificationStats(
    val totalNotifications: Int = 0,
    val totalDismissed: Int = 0,
    val totalTapped: Int = 0,
    val perAppStats: Map<String, AppStat> = emptyMap(),
    val firstUseTimestamp: Long = System.currentTimeMillis()
) {
    val engagementRate: Float
        get() = if (totalNotifications > 0) totalTapped.toFloat() / totalNotifications else 0f

    val dismissalRate: Float
        get() = if (totalNotifications > 0) totalDismissed.toFloat() / totalNotifications else 0f

    val topApps: List<AppStat>
        get() = perAppStats.values.sortedByDescending { it.count }.take(5)
}

data class AppStat(
    val packageName: String = "",
    val appName: String = "",
    val count: Int = 0,
    val lastSeen: Long = 0L
)

private val Context.smartIslandStatsDataStore by androidx.datastore.preferences.preferencesDataStore(
    name = "smart_island_stats"
)
