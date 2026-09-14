package com.memoria.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memoria.mobile.reminders.MemoriaNotifications
import com.memoria.mobile.ui.common.LoadingBox
import com.memoria.mobile.ui.common.repository
import com.memoria.mobile.ui.nav.MemoriaNav
import com.memoria.mobile.ui.theme.MemoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MemoriaNotifications.ensureChannels(this)
        setContent {
            // Collected here rather than inside a screen so changing the palette in
            // Settings repaints the whole app immediately, bottom bar included.
            val preset by (application as MemoriaApp).graph.repository.themePresetFlow
                .collectAsStateWithLifecycle(initialValue = null)
            MemoriaTheme(themePreset = preset) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppEntry()
                }
            }
        }
    }
}

@Composable
private fun AppEntry() {
    val context = LocalContext.current
    val repo = context.repository()
    var ready by remember { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // A corrupt preferences file must not strand the app on this spinner:
        // the login screen can now repair the server address by itself.
        runCatching { repo.bootstrap() }
        loggedIn = repo.isLoggedIn()
        ready = true
    }

    if (!ready) {
        LoadingBox()
    } else {
        MemoriaNav(startLoggedIn = loggedIn)
    }
}
