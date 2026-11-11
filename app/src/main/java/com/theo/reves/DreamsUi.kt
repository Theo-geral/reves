package com.theo.reves

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RevesApp(store: Store, writeRequest: MutableState<LocalDate?>) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingIso by rememberSaveable { mutableStateOf<String?>(null) }
    val dreams by store.dreams.collectAsState()

    // Arrivée depuis le bouton « Oui » de la notification
    val requested = writeRequest.value
    LaunchedEffect(requested) {
        if (requested != null) {
            editingIso = requested.toString()
            tab = 0
            writeRequest.value = null
        }
    }

    // Demande la permission de notifier au premier lancement (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) ReminderScheduler.schedule(ctx) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notifications.hasPermission(ctx)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val editing = editingIso?.let { LocalDate.parse(it) }
    if (editing != null) {
        DreamEditor(
            date = editing,
            initial = dreams[editing]?.text.orEmpty(),
            onBack = { editingIso = null },
            onSave = { text ->
                store.setDream(editing, DreamEntry(dreamed = true, text = text.trim()))
                editingIso = null
            },
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Text("🌙", fontSize = 20.sp) }, label = { Text("Rêves") },
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = { Text("😴", fontSize = 20.sp) }, label = { Text("Sommeil") },
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { tab = 2 },
                        icon = { Text("📊", fontSize = 20.sp) }, label = { Text("Stats") },
                    )
                    NavigationBarItem(
                        selected = tab == 3, onClick = { tab = 3 },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Réglages") },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> DreamsScreen(store, onWrite = { editingIso = it.toString() })
                    1 -> SleepScreen(store)
                    2 -> StatsScreen(store)
                    else -> SettingsScreen(store)
                }
            }
        }
    }
}

@Composable
fun DreamsScreen(store: Store, onWrite: (LocalDate) -> Unit) {
    val dreams by store.dreams.collectAsState()
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mes rêves", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        val todayEntry = dreams[today]
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    todayEntry == null -> {
                        Text("As-tu rêvé cette nuit ?", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onWrite(today) }, modifier = Modifier.weight(1f)) { Text("Oui") }
                            OutlinedButton(
                                onClick = { store.setDream(today, DreamEntry(false, "")) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Non") }
                        }
                    }
                    todayEntry.dreamed -> {
                        Text("Rêve de cette nuit noté ✨", style = MaterialTheme.typography.titleMedium)
                        Text(
                            todayEntry.text.ifBlank { "(pas de description)" },
                            maxLines = 3, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = { onWrite(today) }) { Text("Modifier") }
                    }
                    else -> {
                        Text("Pas de rêve cette nuit", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onWrite(today) }) { Text("En fait si, j'ai rêvé") }
                    }
                }
            }
        }

        MonthCalendar(
            month = month,
            onMonthChange = { month = it },
            dayColor = { d ->
                when (dreams[d]?.dreamed) {
                    true -> DreamColor
                    false -> NoDreamColor
                    null -> null
                }
            },
            onDayClick = { selected = it },
        )
        Legend(listOf(DreamColor to "Rêvé", NoDreamColor to "Pas rêvé"))

        val inMonth = dreams.filterKeys { YearMonth.from(it) == month }
        if (inMonth.isNotEmpty()) {
            Text(
                "${inMonth.count { it.value.dreamed }} nuit(s) avec rêve sur ${inMonth.size} renseignée(s) ce mois-ci",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val sel = selected
    if (sel != null) {
        DreamDayDialog(
            date = sel,
            entry = dreams[sel],
            onDismiss = { selected = null },
            onWrite = { selected = null; onWrite(sel) },
            onNoDream = { store.setDream(sel, DreamEntry(false, "")); selected = null },
            onDelete = { store.setDream(sel, null); selected = null },
        )
    }
}

@Composable
private fun DreamDayDialog(
    date: LocalDate,
    entry: DreamEntry?,
    onDismiss: () -> Unit,
    onWrite: () -> Unit,
    onNoDream: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDate(date), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                when {
                    entry == null -> Text("Rien de noté pour cette nuit.\nAs-tu rêvé ?")
                    entry.dreamed && entry.text.isNotBlank() -> Text(entry.text)
                    entry.dreamed -> Text("Rêve, sans description.", fontStyle = FontStyle.Italic)
                    else -> Text("Pas de rêve cette nuit-là.", fontStyle = FontStyle.Italic)
                }
            }
        },
        confirmButton = {
            if (entry == null) {
                TextButton(onClick = onWrite) { Text("Oui") }
            } else {
                TextButton(onClick = onDismiss) { Text("Fermer") }
            }
        },
        dismissButton = {
            Row {
                when {
                    entry == null -> TextButton(onClick = onNoDream) { Text("Non") }
                    entry.dreamed -> {
                        TextButton(onClick = onDelete) { Text("Supprimer") }
                        TextButton(onClick = onWrite) { Text("Modifier") }
                    }
                    else -> {
                        TextButton(onClick = onDelete) { Text("Supprimer") }
                        TextButton(onClick = onWrite) { Text("J'ai rêvé") }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamEditor(date: LocalDate, initial: String, onBack: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable(date.toString()) { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mon rêve")
                        Text(
                            formatDate(date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().padding(16.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
                placeholder = { Text("Raconte ton rêve tant qu'il est frais : lieux, personnes, émotions…") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSave(text) }, modifier = Modifier.fillMaxWidth()) {
                Text("Enregistrer")
            }
        }
    }
}
