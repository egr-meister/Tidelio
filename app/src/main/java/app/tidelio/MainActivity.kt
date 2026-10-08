package app.tidelio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import app.tidelio.ui.TidelioRoot
import app.tidelio.ui.theme.TidelioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** null until the (local, fast) preference read completes; no artificial delay. */
    private val startWithSetup = MutableStateFlow<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val container = appContainer
        splash.setKeepOnScreenCondition { startWithSetup.value == null }
        lifecycleScope.launch {
            startWithSetup.value = !container.preferences.prefs.first().setupDone
        }
        setContent {
            TidelioTheme {
                val start by startWithSetup.collectAsState()
                start?.let { TidelioRoot(container = container, startWithSetup = it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Launch and foreground return: re-read the device's local date (handles midnight,
        // manual clock changes and time-zone changes while the app was in the background).
        appContainer.todayProvider.refresh()
    }
}
