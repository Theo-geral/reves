package com.theo.reves

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    /** Mis à jour quand on arrive depuis « Oui » ou « Vocal » de la notification (date, enregistrer direct). */
    private val writeRequest = mutableStateOf<Pair<LocalDate, Boolean>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        Notifications.ensureChannel(this)
        ReminderScheduler.schedule(this)
        if (savedInstanceState == null) handleIntent(intent)

        val store = Store.get(this)
        setContent {
            RevesTheme {
                RevesApp(store = store, writeRequest = writeRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Notifications.ACTION_WRITE_DREAM -> {
                Notifications.cancel(this)
                writeRequest.value = Pair(LocalDate.now(), false)
            }
            Notifications.ACTION_RECORD_DREAM -> {
                Notifications.cancel(this)
                writeRequest.value = Pair(LocalDate.now(), true)
            }
        }
    }
}
