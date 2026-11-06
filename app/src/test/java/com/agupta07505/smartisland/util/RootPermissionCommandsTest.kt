/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootPermissionCommandsTest {
    private val packageName = "com.agupta07505.smartisland"
    private val accessibility = "$packageName/com.agupta07505.smartisland.service.SmartIslandOverlayService"
    private val listener = "$packageName/com.agupta07505.smartisland.service.SmartIslandNotificationListenerService"

    @Test
    fun mergePreservesOtherAccessibilityServicesAndAddsOwnOnce() {
        val merged = RootPermissionCommands.mergeColonSeparated(
            "com.bitwarden.authenticator/service:com.lastpass.lpandroid/service:$accessibility",
            accessibility
        )

        assertEquals(
            "com.bitwarden.authenticator/service:com.lastpass.lpandroid/service:$accessibility",
            merged
        )
    }

    @Test
    fun emptySettingIsMergedInsteadOfBeingTreatedAsAnUnreadableValue() {
        assertEquals(accessibility, RootPermissionCommands.mergeColonSeparated("", accessibility))
    }

    @Test
    fun commandsUseTheCurrentUserForEveryUserScopedOperation() {
        val commands = RootPermissionCommands.commands(
            packageName = packageName,
            userId = 10,
            accessibilityComponent = accessibility,
            notificationComponent = listener,
            mergedAccessibility = accessibility,
            mergedNotificationListeners = listener,
            includeRuntimeNotifications = true
        )

        assertEquals(8, commands.size)
        assertTrue(commands.any { it == "appops set --user 10 '$packageName' SYSTEM_ALERT_WINDOW allow" })
        assertTrue(commands.any { it == "appops set --user 10 '$packageName' GET_USAGE_STATS allow" })
        assertTrue(commands.any { it == "settings --user 10 put secure enabled_accessibility_services '$accessibility'" })
        assertTrue(commands.any { it == "settings --user 10 put secure accessibility_enabled 1" })
        assertTrue(commands.any { it == "cmd notification allow_listener --user 10 '$listener'" })
        assertTrue(commands.any { it == "settings --user 10 put secure enabled_notification_listeners '$listener'" })
        assertTrue(commands.any { it == "pm grant --user 10 '$packageName' 'android.permission.POST_NOTIFICATIONS'" })
        assertFalse(commands.any { it.contains("setenforce") || it.contains(" settings delete ") })
    }

    @Test
    fun runtimeNotificationGrantIsOmittedWhenNotRequested() {
        val commands = RootPermissionCommands.commands(
            packageName = packageName,
            userId = 10,
            accessibilityComponent = accessibility,
            notificationComponent = listener,
            mergedAccessibility = accessibility,
            mergedNotificationListeners = listener,
            includeRuntimeNotifications = false
        )

        assertEquals(7, commands.size)
        assertFalse(commands.any { "POST_NOTIFICATIONS" in it })
        assertTrue(commands.any { "SYSTEM_ALERT_WINDOW" in it })
    }

    @Test
    fun secureReadCommandUsesTheRequestedUser() {
        assertEquals(
            "settings --user 12 get secure 'enabled_accessibility_services'",
            RootPermissionCommands.secureGetCommand(12, "enabled_accessibility_services")
        )
    }

    @Test
    fun userIdIsDerivedFromAndroidUidRange() {
        assertEquals(0, RootPermissionCommands.userIdForUid(10_234))
        assertEquals(10, RootPermissionCommands.userIdForUid(1_000_123))
    }

    @Test(expected = IllegalArgumentException::class)
    fun componentRejectsClassOutsideOwnPackage() {
        RootPermissionCommands.component(packageName, "com.other.app.Service")
    }

    @Test
    fun shellQuoteKeepsShellMetacharactersInsideOneArgument() {
        assertEquals("'a'\"'\"'b; rm -rf /'", RootPermissionCommands.quote("a'b; rm -rf /"))
    }
}
