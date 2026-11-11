package com.theo.reves

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun SleepScreen(store: Store) {
    val sleep by store.sleep.collectAsState()
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mon sommeil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        val todayEntry = sleep[today]
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("As-tu bien dormi cette nuit ?", style = MaterialTheme.typography.titleMedium)
                QualityPicker(todayEntry?.quality) { q ->
                    store.setSleep(today, SleepEntry(q, todayEntry?.note.orEmpty()))
                }
                if (todayEntry != null) {
                    if (todayEntry.note.isNotBlank()) {
                        Text(todayEntry.note, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { selected = today }) {
                        Text(if (todayEntry.note.isBlank()) "Ajouter une note" else "Modifier la note")
                    }
                }
            }
        }

        MonthCalendar(
            month = month,
            onMonthChange = { month = it },
            dayColor = { d -> sleep[d]?.let { sleepColor(it.quality) } },
            onDayClick = { selected = it },
        )
        Legend((1..5).map { sleepColor(it) to sleepEmoji(it) })

        val inMonth = sleep.filterKeys { YearMonth.from(it) == month }.values
        if (inMonth.isNotEmpty()) {
            val avg = inMonth.map { it.quality }.average()
            Text(
                "Moyenne du mois : ${String.format(Locale.FRENCH, "%.1f", avg)} / 5 sur ${inMonth.size} nuit(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val sel = selected
    if (sel != null) {
        SleepDayDialog(
            date = sel,
            entry = sleep[sel],
            onDismiss = { selected = null },
            onSave = { store.setSleep(sel, it); selected = null },
            onDelete = { store.setSleep(sel, null); selected = null },
        )
    }
}

@Composable
fun QualityPicker(selected: Int?, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (q in 1..5) {
            val isSel = selected == q
            val shape = RoundedCornerShape(12.dp)
            var mod = Modifier.weight(1f).clip(shape)
                .background(if (isSel) sleepColor(q).copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
            if (isSel) mod = mod.border(2.dp, sleepColor(q), shape)
            Column(
                mod.clickable { onSelect(q) }.padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(sleepEmoji(q), fontSize = 22.sp)
                Text(
                    sleepLabel(q),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SleepDayDialog(
    date: LocalDate,
    entry: SleepEntry?,
    onDismiss: () -> Unit,
    onSave: (SleepEntry) -> Unit,
    onDelete: () -> Unit,
) {
    var quality by remember { mutableStateOf(entry?.quality) }
    var note by remember { mutableStateOf(entry?.note.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDate(date), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("As-tu bien dormi ?", fontStyle = FontStyle.Italic)
                QualityPicker(quality) { quality = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (facultatif)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { quality?.let { onSave(SleepEntry(it, note.trim())) } },
                enabled = quality != null,
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (entry != null) TextButton(onClick = onDelete) { Text("Supprimer") }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
}
