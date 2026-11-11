package com.theo.reves

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val shortDay = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private val shortMonth = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(store: Store) {
    val dreams by store.dreams.collectAsState()
    val sleep by store.sleep.collectAsState()
    var period by rememberSaveable { mutableIntStateOf(30) }

    val today = LocalDate.now()
    val start = today.minusDays(period - 1L)
    fun inPeriod(d: LocalDate) = !d.isBefore(start) && !d.isAfter(today)
    val dIn = dreams.filterKeys { inPeriod(it) }
    val sIn = sleep.filterKeys { inPeriod(it) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Statistiques", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30 to "30 jours", 90 to "3 mois", 365 to "1 an").forEach { (days, label) ->
                FilterChip(selected = period == days, onClick = { period = days }, label = { Text(label) })
            }
        }

        val dreamCount = dIn.count { it.value.dreamed }
        val dreamRate = if (dIn.isEmpty()) "–" else "${(100.0 * dreamCount / dIn.size).roundToInt()} %"
        val sleepAvg = if (sIn.isEmpty()) "–"
        else String.format(Locale.FRENCH, "%.1f", sIn.values.map { it.quality }.average())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Rêves notés", "$dreamCount", Modifier.weight(1f))
            StatTile("Nuits avec rêve", dreamRate, Modifier.weight(1f))
            StatTile("Sommeil moyen", if (sIn.isEmpty()) "–" else "$sleepAvg/5", Modifier.weight(1f))
        }

        ChartCard("Qualité du sommeil", "Points : chaque nuit · ligne : moyenne sur 7 jours") {
            if (sIn.isEmpty()) EmptyChart() else SleepLineChart(sIn, start, today)
        }

        val months = if (period == 365) 12 else 6
        ChartCard("Rêves par mois", "Sur les $months derniers mois") {
            if (dreams.isEmpty()) EmptyChart() else {
                MonthlyDreamBars(dreams, months)
                Legend(listOf(DreamColor to "Avec rêve", NoDreamColor to "Sans rêve"))
            }
        }

        ChartCard("Thèmes fréquents", "Tags les plus présents dans tes rêves sur la période") {
            val counts = dIn.values.filter { it.dreamed }.flatMap { it.tags }
                .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(8)
            if (counts.isEmpty()) EmptyChart("Aucun tag sur cette période") else ThemeBars(counts.map { it.key to it.value })
        }

        ChartCard("Rêve-t-on plus quand on dort bien ?", "Part des nuits avec rêve selon la qualité du sommeil") {
            val both = sIn.keys.count { dIn.containsKey(it) }
            if (both == 0) EmptyChart("Il faut des nuits renseignées dans les deux onglets") else DreamBySleepBars(dIn, sIn)
        }
    }
}

@Composable
private fun ThemeBars(items: List<Pair<String, Int>>) {
    val max = items.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (tag, n) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tag, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                    modifier = Modifier.width(120.dp))
                Box(Modifier.weight(1f).height(14.dp)) {
                    Box(
                        Modifier.fillMaxWidth(n.toFloat() / max).height(14.dp)
                            .clip(RoundedCornerShape(4.dp)).background(DreamColor)
                    )
                }
                Text("$n", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).width(24.dp))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun EmptyChart(message: String = "Pas encore de données sur cette période") {
    Text(message, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp))
}

private fun DrawScope.label(m: TextMeasurer, text: String, style: TextStyle, centerX: Float, topY: Float) {
    val t = m.measure(text, style)
    val x = (centerX - t.size.width / 2f).coerceIn(0f, (size.width - t.size.width).coerceAtLeast(0f))
    drawText(t, topLeft = Offset(x, topY))
}

@Composable
private fun SleepLineChart(data: Map<LocalDate, SleepEntry>, start: LocalDate, end: LocalDate) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val lineColor = MaterialTheme.colorScheme.primary
    val span = ChronoUnit.DAYS.between(start, end).toFloat().coerceAtLeast(1f)
    val points = data.entries.sortedBy { it.key }
    // Moyenne glissante centrée sur 7 jours
    val smooth = points.map { (d, _) ->
        val window = points.filter { ChronoUnit.DAYS.between(d, it.key) in -3..3 }
        d to window.map { it.value.quality }.average().toFloat()
    }

    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val left = 26.dp.toPx(); val right = 8.dp.toPx(); val top = 8.dp.toPx(); val bottom = 22.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        fun x(d: LocalDate) = left + w * ChronoUnit.DAYS.between(start, d) / span
        fun y(q: Float) = top + h * (5f - q) / 4f

        for (q in 1..5) {
            drawLine(grid, Offset(left, y(q.toFloat())), Offset(left + w, y(q.toFloat())), 1.dp.toPx())
            val t = measurer.measure(sleepEmoji(q), style)
            drawText(t, topLeft = Offset(0f, y(q.toFloat()) - t.size.height / 2f))
        }
        val mid = start.plusDays(ChronoUnit.DAYS.between(start, end) / 2)
        listOf(start, mid, end).forEach { d ->
            label(measurer, d.format(shortDay), style, x(d), size.height - 16.dp.toPx())
        }

        if (smooth.size >= 2) {
            val path = Path()
            smooth.forEachIndexed { i, (d, v) ->
                if (i == 0) path.moveTo(x(d), y(v)) else path.lineTo(x(d), y(v))
            }
            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        val r = if (span > 120) 2.5.dp.toPx() else 4.dp.toPx()
        points.forEach { (d, e) -> drawCircle(sleepColor(e.quality), r, Offset(x(d), y(e.quality.toFloat()))) }
    }
}

@Composable
private fun MonthlyDreamBars(dreams: Map<LocalDate, DreamEntry>, months: Int) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    val now = YearMonth.now()
    val stats = (months - 1 downTo 0).map { back ->
        val m = now.minusMonths(back.toLong())
        val e = dreams.filterKeys { YearMonth.from(it) == m }
        Triple(m, e.count { it.value.dreamed }, e.size)
    }
    val maxTotal = stats.maxOf { it.third }.coerceAtLeast(1)

    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val top = 20.dp.toPx(); val bottom = 20.dp.toPx()
        val h = size.height - top - bottom
        val slot = size.width / stats.size
        val barW = slot * 0.6f
        val radius = CornerRadius(4.dp.toPx())
        stats.forEach { (m, dreamed, total) ->
            val i = stats.indexOfFirst { it.first == m }
            val cx = slot * i + slot / 2f
            val totalH = h * total / maxTotal
            val dreamH = h * dreamed / maxTotal
            val baseY = top + h
            if (total > 0) {
                drawRoundRect(NoDreamColor, Offset(cx - barW / 2, baseY - totalH), Size(barW, totalH), radius)
            }
            if (dreamed > 0) {
                drawRoundRect(DreamColor, Offset(cx - barW / 2, baseY - dreamH), Size(barW, dreamH), radius)
            }
            if (total > 0) label(measurer, "$dreamed", valueStyle, cx, baseY - totalH - 18.dp.toPx())
            label(measurer, m.format(shortMonth), style, cx, size.height - 16.dp.toPx())
        }
    }
}

@Composable
private fun DreamBySleepBars(dreams: Map<LocalDate, DreamEntry>, sleep: Map<LocalDate, SleepEntry>) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val stats = (1..5).map { q ->
        val nights = sleep.filter { it.value.quality == q && dreams.containsKey(it.key) }.keys
        val dreamed = nights.count { dreams[it]?.dreamed == true }
        Triple(q, dreamed, nights.size)
    }

    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val top = 20.dp.toPx(); val bottom = 40.dp.toPx()
        val h = size.height - top - bottom
        val slot = size.width / 5f
        val barW = slot * 0.55f
        val radius = CornerRadius(4.dp.toPx())
        stats.forEach { (q, dreamed, n) ->
            val cx = slot * (q - 1) + slot / 2f
            val baseY = top + h
            drawRoundRect(track, Offset(cx - barW / 2, top), Size(barW, h), radius)
            if (n > 0) {
                val rate = dreamed.toFloat() / n
                val bh = h * rate
                if (bh > 0f) drawRoundRect(sleepColor(q), Offset(cx - barW / 2, baseY - bh), Size(barW, bh), radius)
                label(measurer, "${(rate * 100).roundToInt()} %", valueStyle, cx, (baseY - bh - 18.dp.toPx()).coerceAtLeast(0f))
            } else {
                label(measurer, "–", valueStyle, cx, baseY - 18.dp.toPx())
            }
            label(measurer, sleepEmoji(q), style, cx, baseY + 4.dp.toPx())
            label(measurer, "$n nuit${if (n > 1) "s" else ""}", style, cx, baseY + 22.dp.toPx())
        }
    }
}
