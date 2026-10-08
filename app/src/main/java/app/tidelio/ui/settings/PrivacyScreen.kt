@file:OptIn(ExperimentalMaterial3Api::class)

package app.tidelio.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tidelio.R
import app.tidelio.ui.common.ContentWidth

private val sections = listOf(
    "What Tidelio stores" to
        "Only what you enter: water amounts with their date, time and time zone, your daily goal history, " +
        "your quick-add amounts and the animation preference.",
    "Where it is stored" to
        "In the app's private storage on this device. Entries and goals remain on the device. " +
        "Tidelio has no account, no server and no cloud synchronization.",
    "Network" to
        "Tidelio does not request internet access and never transmits your entries, goals or logs. " +
        "It works fully in airplane mode.",
    "Backups" to
        "Cloud backup and device-to-device transfer are disabled for Tidelio's data, so it is not copied " +
        "to backup services. Uninstalling the app or clearing its data removes everything.",
    "Not included" to
        "No advertising, analytics, payments, Firebase, Health Connect, sensors, location or notifications. " +
        "No permissions are requested.",
    "Health information" to
        "Tidelio records amounts you choose to log. It does not collect health measurements, calculate " +
        "hydration requirements or provide medical advice.",
    "Removing your data" to
        "Settings → Clear all local data deletes all entries, goals and preferences after confirmation.",
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        ContentWidth(Modifier.padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                sections.forEach { (title, body) ->
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
