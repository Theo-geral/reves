package com.theo.reves

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object Notifications {
    const val CHANNEL_ID = "morning"
    const val NOTIF_ID = 42
    const val ACTION_WRITE_DREAM = "com.theo.reves.WRITE_DREAM"
    const val ACTION_NO_DREAM = "com.theo.reves.NO_DREAM"
    const val ACTION_RECORD_DREAM = "com.theo.reves.RECORD_DREAM"

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Question du matin", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Te demande chaque matin si tu as rêvé"
            nm.createNotificationChannel(channel)
        }
    }

    /** Affiche « Avez-vous rêvé ? » avec les boutons Oui / Non. */
    @SuppressLint("MissingPermission")
    fun showMorning(ctx: Context) {
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val yesIntent = Intent(ctx, MainActivity::class.java)
            .setAction(ACTION_WRITE_DREAM)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val yes = PendingIntent.getActivity(ctx, 1, yesIntent, flags)

        val voiceIntent = Intent(ctx, MainActivity::class.java)
            .setAction(ACTION_RECORD_DREAM)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val voice = PendingIntent.getActivity(ctx, 4, voiceIntent, flags)

        val noIntent = Intent(ctx, DreamActionReceiver::class.java).setAction(ACTION_NO_DREAM)
        val no = PendingIntent.getBroadcast(ctx, 2, noIntent, flags)

        val openIntent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(ctx, 3, openIntent, flags)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle("Avez-vous rêvé ?")
            .setContentText("Notez-le tant que c'est encore frais")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Oui", yes)
            .addAction(0, "Non", no)
            .addAction(0, "🎙 Vocal", voice)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            // permission retirée entre-temps
        }
    }

    fun cancel(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
    }
}

object ReminderScheduler {
    /** Programme la prochaine notification à l'heure choisie (6h55 par défaut). */
    fun schedule(ctx: Context) {
        val (hour, minute) = Store.get(ctx).reminder.value
        val now = ZonedDateTime.now()
        var next = now.toLocalDate().atTime(hour, minute).atZone(ZoneId.systemDefault())
        if (!next.isAfter(now.plusSeconds(30))) next = next.plusDays(1)

        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = next.toInstant().toEpochMilli()
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}

/** Déclenché chaque matin par l'alarme. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Pas de notif si on a déjà répondu aujourd'hui
        if (Store.get(context).dreams.value[LocalDate.now()] == null) {
            Notifications.showMorning(context)
        }
        ReminderScheduler.schedule(context)
    }
}

/** Bouton « Non » de la notification : enregistre « pas de rêve » sans ouvrir l'appli. */
class DreamActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Notifications.ACTION_NO_DREAM) {
            Store.get(context).setDream(LocalDate.now(), DreamEntry(dreamed = false, text = ""))
            Notifications.cancel(context)
        }
    }
}

/** Reprogramme l'alarme après un redémarrage, une mise à jour ou un changement d'heure. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.schedule(context)
    }
}
