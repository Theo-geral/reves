package com.theo.reves

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val NightScheme = darkColorScheme(
    primary = Color(0xFFB9A6FF),
    onPrimary = Color(0xFF21184A),
    primaryContainer = Color(0xFF3A2F6E),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFFFD86B),
    onSecondary = Color(0xFF3A2E00),
    background = Color(0xFF110F1D),
    onBackground = Color(0xFFE8E4F3),
    surface = Color(0xFF110F1D),
    onSurface = Color(0xFFE8E4F3),
    surfaceVariant = Color(0xFF2A2740),
    onSurfaceVariant = Color(0xFFB7B1CC),
    surfaceContainerLowest = Color(0xFF0D0B17),
    surfaceContainerLow = Color(0xFF161424),
    surfaceContainer = Color(0xFF1A1829),
    surfaceContainerHigh = Color(0xFF1F1C31),
    surfaceContainerHighest = Color(0xFF242138),
    outline = Color(0xFF6F6A88),
)

@Composable
fun RevesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NightScheme, content = content)
}

val DreamColor = Color(0xFF9C7CFF)
val NoDreamColor = Color(0xFF4A4760)

fun sleepColor(q: Int): Color = when (q) {
    1 -> Color(0xFFE5534B)
    2 -> Color(0xFFF0883E)
    3 -> Color(0xFFD4B13A)
    4 -> Color(0xFF7BC67E)
    else -> Color(0xFF3FB27F)
}

fun sleepEmoji(q: Int): String = when (q) {
    1 -> "😫"
    2 -> "😕"
    3 -> "😐"
    4 -> "🙂"
    else -> "😴"
}

fun sleepLabel(q: Int): String = when (q) {
    1 -> "Très mal"
    2 -> "Mal"
    3 -> "Moyen"
    4 -> "Bien"
    else -> "Très bien"
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)

fun formatDate(d: LocalDate): String = d.format(dayFormatter).replaceFirstChar { it.uppercase() }
fun monthTitle(m: YearMonth): String = m.format(monthFormatter).replaceFirstChar { it.uppercase() }
