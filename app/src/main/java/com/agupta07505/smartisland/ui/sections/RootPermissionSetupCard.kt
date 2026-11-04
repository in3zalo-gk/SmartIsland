/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.util.RootPermissionSetup
import com.agupta07505.smartisland.util.RootPermissionSetupResult
import com.agupta07505.smartisland.util.RootPermissionStatus
import kotlinx.coroutines.launch

/**
 * Opt-in root permission setup. The helper uses a package-scoped command allowlist and
 * verifies outcomes; manual permission controls remain available below this card.
 */
@Composable
fun RootPermissionSetupCard(onRefreshPermissions: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RootPermissionSetupResult?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Root-assisted setup",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Optional. After you approve superuser access, Smart Island only configures this app for the current Android user. Existing enabled services are preserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                enabled = !isRunning,
                onClick = {
                    isRunning = true
                    scope.launch {
                        result = RootPermissionSetup.grantAllForThisApp(
                            context = androidx.compose.ui.platform.LocalContext.current
                        )
                        isRunning = false
                        onRefreshPermissions()
                    }
                }
            ) {
                Text(if (isRunning) "Setting up…" else "Set up with root")
            }
            result?.let { setupResult ->
                Text(
                    text = setupResult.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                setupResult.permissions.forEach { permission ->
                    val status = when (permission.status) {
                        RootPermissionStatus.GRANTED -> "Granted"
                        RootPermissionStatus.NOT_GRANTED -> "Not granted"
                        RootPermissionStatus.UNSUPPORTED -> "Unavailable"
                    }
                    Text(
                        text = "${permission.label}: $status — ${permission.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
