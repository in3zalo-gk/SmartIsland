/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Results shown after a user explicitly chooses root-assisted permission setup. */
data class RootPermissionSetupResult(
    val rootAvailable: Boolean,
    val message: String,
    val permissions: List<RootPermissionResult>
)

data class RootPermissionResult(
    val label: String,
    val status: RootPermissionStatus,
    val detail: String
)

enum class RootPermissionStatus { GRANTED, NOT_GRANTED, UNSUPPORTED }

/**
 * Builds only the small, package-scoped command allowlist used by [RootPermissionSetup].
 * This has no shell execution so it can be unit tested independently.
 */
internal object RootPermissionCommands {
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    fun userIdForUid(uid: Int): Int {
        require(uid >= 0) { "UID must not be negative" }
        return uid / PER_USER_RANGE
    }

    fun component(packageName: String, className: String): String {
        requireOwnedPackage(packageName)
        require(className.startsWith("$packageName.")) { "Component must belong to the app package" }
        return "$packageName/$className"
    }

    fun mergeColonSeparated(currentValue: String, ownComponent: String): String {
        require(ownComponent.contains('/')) { "Component must be flattened" }
        val entries = currentValue
            .split(':')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toMutableList()
        if (ownComponent !in entries) entries += ownComponent
        return entries.joinToString(":")
    }

    fun quote(argument: String): String = "'${argument.replace("'", "'\"'\"'")}'"

    fun secureGetCommand(userId: Int, setting: String): String {
        require(userId >= 0)
        return "settings --user $userId get secure ${quote(setting)}"
    }

    fun commands(
        packageName: String,
        userId: Int,
        accessibilityComponent: String,
        notificationComponent: String,
        mergedAccessibility: String,
        mergedNotificationListeners: String,
        includeRuntimeNotifications: Boolean
    ): List<String> {
        requireOwnedPackage(packageName)
        require(userId >= 0) { "User ID must not be negative" }
        require(accessibilityComponent.startsWith("$packageName/"))
        require(notificationComponent.startsWith("$packageName/"))
        val pkg = quote(packageName)
        return buildList {
            add("appops set --user $userId $pkg SYSTEM_ALERT_WINDOW allow")
            add("appops set --user $userId $pkg GET_USAGE_STATS allow")
            // Preserve every existing entry and add only Smart Island's own component.
            add("settings --user $userId put secure enabled_accessibility_services ${quote(mergedAccessibility)}")
            // Android only activates services when both the global flag and own entry are present.
            add("settings --user $userId put secure accessibility_enabled 1")
            add("cmd notification allow_listener --user $userId ${quote(notificationComponent)}")
            add("settings --user $userId put secure enabled_notification_listeners ${quote(mergedNotificationListeners)}")
            if (includeRuntimeNotifications) {
                // A shell argument, not an Android API call. The caller enables this only on API 33+.
                // Keep this command builder platform-independent so its allowlist is unit-testable.
                add("pm grant --user $userId $pkg ${quote("android.permission.POST_NOTIFICATIONS")}")
            }
            add("dumpsys deviceidle whitelist ${quote("+$packageName")}")
        }
    }

    private fun requireOwnedPackage(packageName: String) {
        require(packagePattern.matches(packageName)) { "Invalid package name" }
    }

    private const val PER_USER_RANGE = 100_000
}

/**
 * Explicit-consent root setup. It never accepts shell text from UI and never changes global
 * SELinux/security policy or unrelated application preferences.
 */
object RootPermissionSetup {
    private const val commandTimeoutMillis = 8_000L
    private const val settleDelayMillis = 350L
    private const val notificationListenersSetting = "enabled_notification_listeners"
    private const val accessibilityEnabledSetting = "accessibility_enabled"

    suspend fun grantAllForThisApp(context: Context): RootPermissionSetupResult = withContext(Dispatchers.IO) {
        val rootProbe = runAsRoot("id -u")
        if (!rootProbe.succeeded || rootProbe.stdout.trim() != "0") {
            return@withContext RootPermissionSetupResult(
                rootAvailable = false,
                message = "Superuser access was not granted. You can continue with the manual setup.",
                permissions = emptyList()
            )
        }

        val packageName = context.packageName
        val userId = RootPermissionCommands.userIdForUid(Process.myUid())
        val accessibilityComponent = RootPermissionCommands.component(
            packageName,
            SmartIslandOverlayService::class.java.name
        )
        val notificationComponent = RootPermissionCommands.component(
            packageName,
            SmartIslandNotificationListenerService::class.java.name
        )
        val accessibilityRead = readSecureSetting(context, userId, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val notificationRead = readSecureSetting(context, userId, notificationListenersSetting)
        if (accessibilityRead !is SecureSettingRead.Value || notificationRead !is SecureSettingRead.Value) {
            return@withContext RootPermissionSetupResult(
                rootAvailable = true,
                message = "Smart Island could not safely read this user's existing enabled services, so nothing was changed. Use manual setup instead.",
                permissions = verify(context, userId, accessibilityComponent, notificationComponent)
            )
        }

        val commands = RootPermissionCommands.commands(
            packageName = packageName,
            userId = userId,
            accessibilityComponent = accessibilityComponent,
            notificationComponent = notificationComponent,
            mergedAccessibility = RootPermissionCommands.mergeColonSeparated(accessibilityRead.value, accessibilityComponent),
            mergedNotificationListeners = RootPermissionCommands.mergeColonSeparated(notificationRead.value, notificationComponent),
            includeRuntimeNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
        val commandsSucceeded = commands.map(::runAsRoot).all { it.succeeded }
        // Settings and app-ops notifications are asynchronous on some Android builds.
        Thread.sleep(settleDelayMillis)

        val results = verify(context, userId, accessibilityComponent, notificationComponent)
        RootPermissionSetupResult(
            rootAvailable = true,
            message = if (commandsSucceeded && results.all { it.status != RootPermissionStatus.NOT_GRANTED }) {
                "Root setup completed. Check the results below."
            } else {
                "Root setup completed partially. Use the manual buttons for anything still not granted."
            },
            permissions = results
        )
    }

    private fun verify(
        context: Context,
        userId: Int,
        accessibilityComponent: String,
        notificationComponent: String
    ): List<RootPermissionResult> {
        val accessibilityEntries = readSecureSetting(context, userId, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val accessibilityGloballyEnabled = readSecureSetting(context, userId, accessibilityEnabledSetting)
        val accessibilityResult = when {
            accessibilityEntries !is SecureSettingRead.Value || accessibilityGloballyEnabled !is SecureSettingRead.Value ->
                unsupported("Accessibility", "Could not safely read this user's secure setting.")
            else -> result(
                "Accessibility",
                accessibilityGloballyEnabled.value.trim() == "1" && isComponentEnabled(accessibilityEntries.value, accessibilityComponent)
            )
        }
        val listenerEntries = readSecureSetting(context, userId, notificationListenersSetting)
        val listenerResult = when (listenerEntries) {
            is SecureSettingRead.Value -> result("Notification listener", isComponentEnabled(listenerEntries.value, notificationComponent))
            SecureSettingRead.Unavailable -> unsupported("Notification listener", "Could not safely read this user's secure setting.")
        }
        return listOf(
            accessibilityResult,
            listenerResult,
            result("Display over other apps", canDrawOverlays(context)),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result("Notifications", context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            } else {
                RootPermissionResult("Notifications", RootPermissionStatus.UNSUPPORTED, "Not a runtime permission on this Android version.")
            },
            result("Usage access", hasUsageAccess(context)),
            result("Battery optimization exemption", isIgnoringBatteryOptimizations(context))
        )
    }

    private fun result(label: String, granted: Boolean): RootPermissionResult = RootPermissionResult(
        label = label,
        status = if (granted) RootPermissionStatus.GRANTED else RootPermissionStatus.NOT_GRANTED,
        detail = if (granted) "Granted" else "Not granted; use manual setup if your device blocks this command."
    )

    private fun unsupported(label: String, detail: String) = RootPermissionResult(label, RootPermissionStatus.UNSUPPORTED, detail)

    private fun hasUsageAccess(context: Context): Boolean = runCatching {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean = runCatching {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)

    private fun canDrawOverlays(context: Context): Boolean = runCatching {
        Settings.canDrawOverlays(context)
    }.getOrDefault(false)

    /** A null value is a valid empty setting; only an exception triggers the root read fallback. */
    private fun readSecureSetting(context: Context, userId: Int, name: String): SecureSettingRead {
        try {
            return SecureSettingRead.Value(Settings.Secure.getString(context.contentResolver, name).orEmpty())
        } catch (_: SecurityException) {
            // Fall through to a root read for the same Android user only.
        } catch (_: Exception) {
            return SecureSettingRead.Unavailable
        }
        val rootRead = runAsRoot(RootPermissionCommands.secureGetCommand(userId, name))
        if (!rootRead.succeeded) return SecureSettingRead.Unavailable
        val value = rootRead.stdout.trim()
        return SecureSettingRead.Value(if (value == "null") "" else value)
    }

    private fun isComponentEnabled(value: String, component: String): Boolean = value
        .split(':')
        .map(String::trim)
        .any { it == component }

    private fun runAsRoot(command: String): RootCommandOutput {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(commandTimeoutMillis, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroy()
                if (!process.waitFor(250, TimeUnit.MILLISECONDS)) process.destroyForcibly()
                RootCommandOutput(false, -1, "")
            } else {
                RootCommandOutput(true, process.exitValue(), process.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (_: Exception) {
            RootCommandOutput(false, -1, "")
        }
    }

    private data class RootCommandOutput(val completed: Boolean, val exitCode: Int, val stdout: String) {
        val succeeded: Boolean get() = completed && exitCode == 0
    }

    private sealed interface SecureSettingRead {
        data class Value(val value: String) : SecureSettingRead
        data object Unavailable : SecureSettingRead
    }
}
