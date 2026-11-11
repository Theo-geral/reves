package com.theo.reves

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/**
 * Calendrier mensuel. [dayColor] renvoie la couleur de la pastille d'un jour
 * (null = rien d'enregistré). Les jours futurs ne sont pas cliquables.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    dayColor: (LocalDate) -> Color?,
    onDayClick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    Card {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mois précédent")
                }
                Text(
                    monthTitle(month),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = { onMonthChange(month.plusMonths(1)) },
                    enabled = month < YearMonth.now(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mois suivant")
                }
            }
            Row(Modifier.padding(vertical = 4.dp)) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val offset = month.atDay(1).dayOfWeek.value - 1 // lundi = 0
            val days = month.lengthOfMonth()
            val rows = (offset + days + 6) / 7
            for (r in 0 until rows) {
                Row {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - offset + 1
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).padding(3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dayNum in 1..days) {
                                DayCell(
                                    date = month.atDay(dayNum),
                                    today = today,
                                    color = dayColor(month.atDay(dayNum)),
                                    onClick = onDayClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, color: Color?, onClick: (LocalDate) -> Unit) {
    val future = date.isAfter(today)
    var mod = Modifier.fillMaxSize().clip(CircleShape)
    if (color != null) mod = mod.background(color)
    if (date == today) mod = mod.border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
    mod = mod.clickable(enabled = !future) { onClick(date) }

    val textColor = when {
        future -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        color != null -> if (color.luminance() > 0.45f) Color(0xFF15131F) else Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(mod, contentAlignment = Alignment.Center) {
        Text(
            "${date.dayOfMonth}",
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (color != null || date == today) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
fun Legend(items: List<Pair<Color, String>>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (color, label) ->
            if (i > 0) Spacer(Modifier.width(16.dp))
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
