package com.theo.reves

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.LocalDate

/** Une nuit côté rêves : a-t-on rêvé, et la description éventuelle. */
data class DreamEntry(val dreamed: Boolean, val text: String)

/** Une nuit côté sommeil : qualité de 1 (très mal) à 5 (très bien) + note libre. */
data class SleepEntry(val quality: Int, val note: String)

/**
 * Stockage local (sur le téléphone uniquement), en JSON dans les SharedPreferences.
 * Les données sont incluses dans la sauvegarde automatique Android.
 */
class Store private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("reves", Context.MODE_PRIVATE)

    private val _dreams = MutableStateFlow(readDreams())
    val dreams: StateFlow<Map<LocalDate, DreamEntry>> = _dreams.asStateFlow()

    private val _sleep = MutableStateFlow(readSleep())
    val sleep: StateFlow<Map<LocalDate, SleepEntry>> = _sleep.asStateFlow()

    private val _reminder = MutableStateFlow(
        Pair(prefs.getInt(KEY_HOUR, 6), prefs.getInt(KEY_MINUTE, 55))
    )
    val reminder: StateFlow<Pair<Int, Int>> = _reminder.asStateFlow()

    @Synchronized
    fun setDream(date: LocalDate, entry: DreamEntry?) {
        val map = _dreams.value.toMutableMap()
        if (entry == null) map.remove(date) else map[date] = entry
        _dreams.value = map
        val json = JSONObject()
        map.forEach { (d, e) ->
            json.put(d.toString(), JSONObject().put("dreamed", e.dreamed).put("text", e.text))
        }
        prefs.edit().putString(KEY_DREAMS, json.toString()).apply()
    }

    @Synchronized
    fun setSleep(date: LocalDate, entry: SleepEntry?) {
        val map = _sleep.value.toMutableMap()
        if (entry == null) map.remove(date) else map[date] = entry
        _sleep.value = map
        val json = JSONObject()
        map.forEach { (d, e) ->
            json.put(d.toString(), JSONObject().put("quality", e.quality).put("note", e.note))
        }
        prefs.edit().putString(KEY_SLEEP, json.toString()).apply()
    }

    fun setReminder(hour: Int, minute: Int) {
        _reminder.value = Pair(hour, minute)
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
    }

    fun exportJson(): String {
        val dreams = JSONObject(prefs.getString(KEY_DREAMS, null) ?: "{}")
        val sleep = JSONObject(prefs.getString(KEY_SLEEP, null) ?: "{}")
        return JSONObject().put("reves", dreams).put("sommeil", sleep).toString(2)
    }

    private fun readDreams(): Map<LocalDate, DreamEntry> {
        val raw = prefs.getString(KEY_DREAMS, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val out = HashMap<LocalDate, DreamEntry>()
            json.keys().forEach { key ->
                val o = json.getJSONObject(key)
                out[LocalDate.parse(key)] = DreamEntry(o.optBoolean("dreamed"), o.optString("text"))
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun readSleep(): Map<LocalDate, SleepEntry> {
        val raw = prefs.getString(KEY_SLEEP, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val out = HashMap<LocalDate, SleepEntry>()
            json.keys().forEach { key ->
                val o = json.getJSONObject(key)
                out[LocalDate.parse(key)] = SleepEntry(o.optInt("quality", 3), o.optString("note"))
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val KEY_DREAMS = "dreams"
        private const val KEY_SLEEP = "sleep"
        private const val KEY_HOUR = "reminder_hour"
        private const val KEY_MINUTE = "reminder_minute"

        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }
}
