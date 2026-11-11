package com.theo.reves

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

object AudioFiles {
    fun dir(ctx: Context): File = File(ctx.filesDir, "audio").apply { mkdirs() }
    fun file(ctx: Context, name: String): File = File(dir(ctx), name)
    fun delete(ctx: Context, name: String?) {
        if (name != null) file(ctx, name).delete()
    }
}

fun formatDuration(ms: Int): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Enregistreur micro → fichier .m4a (AAC). */
class AudioRecorder(private val ctx: Context) {
    private var recorder: MediaRecorder? = null
    private var current: File? = null
    val isRecording: Boolean get() = recorder != null

    fun start(file: File): Boolean {
        return try {
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            current = file
            true
        } catch (e: Exception) {
            file.delete()
            false
        }
    }

    /** Arrête et renvoie true si le fichier est exploitable. */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        val ok = try {
            r.stop(); true
        } catch (e: RuntimeException) {
            current?.delete(); false
        }
        r.release()
        return ok
    }

    fun cancel() {
        stop()
        current?.delete()
    }
}

@Composable
fun AudioPlayer(file: File, modifier: Modifier = Modifier) {
    val path = file.absolutePath
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(path) { mutableStateOf(false) }
    var position by remember(path) { mutableIntStateOf(0) }
    val duration = remember(path) {
        try {
            val r = MediaMetadataRetriever()
            r.setDataSource(path)
            val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
            r.release()
            d
        } catch (e: Exception) {
            0
        }
    }

    DisposableEffect(path) {
        onDispose {
            player?.release()
            player = null
        }
    }
    LaunchedEffect(playing) {
        while (playing) {
            position = player?.currentPosition ?: 0
            delay(100)
        }
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = {
            try {
                val p = player ?: MediaPlayer().also { mp ->
                    mp.setDataSource(path)
                    mp.prepare()
                    mp.setOnCompletionListener { playing = false; position = 0 }
                    player = mp
                }
                if (playing) {
                    p.pause(); playing = false
                } else {
                    p.start(); playing = true
                }
            } catch (e: Exception) {
                playing = false
            }
        }) {
            if (playing) Text("❚❚", fontWeight = FontWeight.Bold)
            else Icon(Icons.Filled.PlayArrow, contentDescription = "Écouter")
        }
        Spacer(Modifier.width(10.dp))
        LinearProgressIndicator(
            progress = { if (duration > 0) position.toFloat() / duration else 0f },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (playing || position > 0) formatDuration(position) else formatDuration(duration),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
