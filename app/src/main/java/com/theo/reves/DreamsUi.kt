package com.theo.reves

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RevesApp(store: Store, writeRequest: MutableState<Pair<LocalDate, Boolean>?>) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingIso by rememberSaveable { mutableStateOf<String?>(null) }
    var autoRecord by remember { mutableStateOf(false) }
    val dreams by store.dreams.collectAsState()

    // Arrivée depuis « Oui » ou « Vocal » dans la notification
    val requested = writeRequest.value
    LaunchedEffect(requested) {
        if (requested != null) {
            editingIso = requested.first.toString()
            autoRecord = requested.second
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
        val entry = dreams[editing]
        DreamEditor(
            date = editing,
            initial = entry?.takeIf { it.dreamed },
            autoRecord = autoRecord,
            onBack = { editingIso = null; autoRecord = false },
            onSave = { text, tags, audio ->
                store.setDream(editing, DreamEntry(dreamed = true, text = text.trim(), tags = tags, audio = audio))
                editingIso = null
                autoRecord = false
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPills(tags: List<String>) {
    if (tags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.forEach { tag ->
            Text(
                tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun DreamsScreen(store: Store, onWrite: (LocalDate) -> Unit) {
    val ctx = LocalContext.current
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
                        if (todayEntry.text.isNotBlank()) {
                            Text(
                                todayEntry.text, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TagPills(todayEntry.tags)
                        todayEntry.audio?.let { AudioPlayer(AudioFiles.file(ctx, it), Modifier.fillMaxWidth()) }
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
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDate(date), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    entry == null -> Text("Rien de noté pour cette nuit.\nAs-tu rêvé ?")
                    entry.dreamed -> {
                        if (entry.text.isNotBlank()) Text(entry.text)
                        else if (entry.audio == null) Text("Rêve, sans description.", fontStyle = FontStyle.Italic)
                        entry.audio?.let { AudioPlayer(AudioFiles.file(ctx, it), Modifier.fillMaxWidth()) }
                        TagPills(entry.tags)
                    }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DreamEditor(
    date: LocalDate,
    initial: DreamEntry?,
    autoRecord: Boolean,
    onBack: () -> Unit,
    onSave: (text: String, tags: List<String>, audio: String?) -> Unit,
) {
    val ctx = LocalContext.current
    var text by rememberSaveable(date.toString()) { mutableStateOf(initial?.text.orEmpty()) }

    // --- Tags : automatiques (désactivables) + ajoutés à la main ---
    val initialAuto = remember { AutoTags.detect(initial?.text.orEmpty()) }
    var removed by remember { mutableStateOf(initialAuto.filter { initial != null && it !in initial.tags }) }
    var custom by remember { mutableStateOf(initial?.tags.orEmpty().filter { it !in initialAuto }) }
    val autoTags = remember(text) { AutoTags.detect(text) }
    val finalTags = (autoTags.filter { it !in removed } + custom).distinct()
    var addingTag by remember { mutableStateOf(false) }

    // --- Note vocale ---
    val recorder = remember { AudioRecorder(ctx) }
    var audioName by remember { mutableStateOf(initial?.audio) }
    var pendingName by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }

    fun startRecording() {
        val name = "reve_${date}_${System.currentTimeMillis()}.m4a"
        if (recorder.start(AudioFiles.file(ctx, name))) {
            pendingName = name
            recording = true
        } else {
            Toast.makeText(ctx, "Impossible d'utiliser le micro", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        if (!recording) return
        recording = false
        val ok = recorder.stop()
        val newName = pendingName
        pendingName = null
        if (ok && newName != null) {
            // Remplace un enregistrement précédent non encore sauvegardé
            if (audioName != null && audioName != initial?.audio) AudioFiles.delete(ctx, audioName)
            audioName = newName
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
        else Toast.makeText(ctx, "Autorise le micro pour les notes vocales", Toast.LENGTH_SHORT).show()
    }

    fun requestRecording() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val dictation = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            text = if (text.isBlank()) spoken.replaceFirstChar { it.uppercase() } else text.trimEnd() + " " + spoken
        }
    }

    fun discardAndLeave() {
        if (recording) {
            recording = false
            recorder.cancel()
        }
        if (audioName != null && audioName != initial?.audio) AudioFiles.delete(ctx, audioName)
        onBack()
    }

    LaunchedEffect(recording) {
        elapsed = 0
        while (recording) {
            delay(1000)
            elapsed++
        }
    }
    DisposableEffect(Unit) {
        onDispose { if (recorder.isRecording) recorder.cancel() }
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (autoRecord) requestRecording() else focus.requestFocus()
    }
    BackHandler { discardAndLeave() }

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
                    IconButton(onClick = { discardAndLeave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
                placeholder = { Text("Raconte ton rêve tant qu'il est frais : lieux, personnes, émotions…") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            // Tags
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                autoTags.forEach { tag ->
                    val on = tag !in removed
                    FilterChip(
                        selected = on,
                        onClick = { removed = if (on) removed + tag else removed - tag },
                        label = { Text(tag) },
                    )
                }
                custom.forEach { tag ->
                    FilterChip(
                        selected = true,
                        onClick = { custom = custom - tag },
                        label = { Text("$tag  ✕") },
                    )
                }
                AssistChip(onClick = { addingTag = true }, label = { Text("+ Tag") })
            }

            // Note vocale + dictée
            when {
                recording -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFE5534B)))
                    Spacer(Modifier.width(8.dp))
                    Text("Enregistrement… ${formatDuration(elapsed * 1000)}", Modifier.weight(1f))
                    Button(
                        onClick = { stopRecording() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5534B)),
                    ) { Text("Arrêter") }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val name = audioName
                    if (name != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AudioPlayer(AudioFiles.file(ctx, name), Modifier.weight(1f))
                            IconButton(onClick = {
                                if (name != initial?.audio) AudioFiles.delete(ctx, name)
                                audioName = null
                            }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer la note vocale") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { requestRecording() }, modifier = Modifier.weight(1f)) {
                            Text(if (name == null) "🎙 Note vocale" else "🎙 Réenregistrer")
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    dictation.launch(
                                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Raconte ton rêve…")
                                    )
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(ctx, "Reconnaissance vocale indisponible", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("🗣 Dicter") }
                    }
                }
            }

            Button(
                onClick = {
                    stopRecording()
                    onSave(text, finalTags, audioName)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }
        }
    }

    if (addingTag) {
        var newTag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addingTag = false },
            title = { Text("Nouveau tag") },
            text = {
                OutlinedTextField(
                    value = newTag, onValueChange = { newTag = it }, singleLine = true,
                    placeholder = { Text("ex. Lannion, ex, vélo…") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = newTag.trim().replaceFirstChar { it.uppercase() }
                        if (t.isNotEmpty() && t !in finalTags) custom = custom + t
                        removed = removed - t
                        addingTag = false
                    },
                    enabled = newTag.isNotBlank(),
                ) { Text("Ajouter") }
            },
            dismissButton = { TextButton(onClick = { addingTag = false }) { Text("Annuler") } },
        )
    }
}
