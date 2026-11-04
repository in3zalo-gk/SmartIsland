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
    private val notification = "$packageName/com.agupta07505.smartisland.service.SmartIslandNotificationListenerService"

    @Test
    fun userIdForUid_usesAndroidPerUserRange() {
        assertEquals(0, RootPermissionCommands.userIdForUid(10_999))
        assertEquals(10, RootPermissionCommands.userIdForUid(1_000_123))
    }

    @Test(expected = IllegalArgumentException::class)
    fun userIdForUid_rejectsNegativeUid() {
        RootPermissionCommands.userIdForUid(-1)
    }

    @Test
    fun component_acceptsOnlyAppOwnedClass() {
        assertEquals(
            accessibility,
            RootPermissionCommands.component(
                packageName,
                "com.agupta07505.smartisland.service.SmartIslandOverlayService"
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun component_rejectsForeignClass() {
        RootPermissionCommands.component(packageName, "com.other.app.Service")
    }

    @Test
    fun mergeColonSeparated_preservesExistingEntriesAndAvoidsDuplicate() {
        val existing = "one/service:two/service"
        assertEquals(
            "one/service:two/service:$accessibility",
            RootPermissionCommands.mergeColonSeparated(existing, accessibility)
        )
        assertEquals(existing, RootPermissionCommands.mergeColonSeparated(existing, "two/service"))
    }

    @Test
    fun commands_areScopedToTheCurrentUserAndPackage() {
        val commands = RootPermissionCommands.commands(
            packageName = packageName,
            userId = 10,
            accessibilityComponent = accessibility,
            notificationComponent = notification,
            mergedAccessibility = accessibility,
            mergedNotificationListeners = notification,
            includeRuntimeNotifications = true
        )

        assertTrue(commands.all { it.contains("--user 10") || it.startsWith("dumpsys deviceidle whitelist") })
        assertTrue(commands.any { it.contains("SYSTEM_ALERT_WINDOW allow") })
        assertTrue(commands.any { it.contains("POST_NOTIFICATIONS") })
        assertTrue(commands.any { it.contains("enabled_accessibility_services") })
        assertTrue(commands.any { it.contains("enabled_notification_listeners") })
        assertFalse(commands.any { it.contains("com.other") })
    }

    @Test
    fun commands_skipNotificationRuntimeGrantBeforeAndroid13() {
        val commands = RootPermissionCommands.commands(
            packageName = packageName,
            userId = 0,
            accessibilityComponent = accessibility,
            notificationComponent = notification,
            mergedAccessibility = accessibility,
            mergedNotificationListeners = notification,
            includeRuntimeNotifications = false
        )

        assertFalse(commands.any { it.contains("POST_NOTIFICATIONS") })
    }
}
