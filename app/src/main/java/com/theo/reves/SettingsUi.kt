package com.theo.reves

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun SettingsScreen(store: Store) {
    val ctx = LocalContext.current
    val reminder by store.reminder.collectAsState()
    var notifAllowed by remember { mutableStateOf(Notifications.hasPermission(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifAllowed = Notifications.hasPermission(ctx)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Réglages", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Rappel du matin", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Chaque jour, une notification « Avez-vous rêvé ? » avec Oui / Non.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        TimePickerDialog(ctx, { _, h, m ->
                            store.setReminder(h, m)
                            ReminderScheduler.schedule(ctx)
                        }, reminder.first, reminder.second, true).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Heure : %02dh%02d".format(reminder.first, reminder.second))
                }
                OutlinedButton(
                    onClick = {
                        if (Notifications.hasPermission(ctx)) Notifications.showMorning(ctx)
                        else Toast.makeText(ctx, "Notifications désactivées", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Tester la notification") }
            }
        }

        if (!notifAllowed) {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚠️ Notifications désactivées", style = MaterialTheme.typography.titleMedium)
                    Text("Sans elles, pas de question le matin.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        )
                    }) { Text("Les activer") }
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("La notif n'arrive pas ?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Certains téléphones coupent les applis en arrière-plan. Mets « Rêves » sur « Sans restriction » dans l'optimisation de batterie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = {
                    try {
                        ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Réglage introuvable", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Optimisation de batterie") }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mes données", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tout reste sur ton téléphone. Tu peux exporter une copie (rêves + sommeil) en texte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Export Rêves")
                        .putExtra(Intent.EXTRA_TEXT, store.exportJson())
                    ctx.startActivity(Intent.createChooser(send, "Exporter mes données"))
                }) { Text("Exporter") }
            }
        }
    }
}
