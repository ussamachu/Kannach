package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

/**
 * Handles scheduling and displaying local OS notifications for:
 * 1. Daily writing reminders (with rotating inspirational messages).
 * 2. Time Capsule delivery on the unlocked date.
 */
class KannachAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: TYPE_REMINDER
        val repo = KannachRepository(context)
        val lang = repo.getLanguage()
        val strings = I18n.forLanguage(lang)

        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (type) {
            TYPE_REMINDER -> {
                val reminder = repo.getReminder()
                if (!reminder.enabled) return

                val msgs = strings.reminderMsgs
                var idx = Random.nextInt(msgs.size)
                if (msgs.size > 1 && idx == reminder.lastIdx) {
                    idx = (idx + 1) % msgs.size
                }
                repo.updateReminderLastIdx(idx)

                val message = msgs[idx]

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(strings.appTitle)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(NOTIF_ID_REMINDER, notif)

                // Reschedule for next day
                NotificationHelper.scheduleDailyReminder(context, reminder.time)
            }

            TYPE_CAPSULE -> {
                val capsuleTitle = strings.capsuleReadyBanner
                val capsuleSub = strings.capsuleTitle

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(capsuleTitle)
                    .setContentText(capsuleSub)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                val capsuleId = intent.getIntExtra(EXTRA_CAPSULE_ID, 2000)
                notificationManager.notify(capsuleId, notif)
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Kannach Notifications"
            val descriptionText = "Daily writing reminders and time capsule notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "kannach_daily_channel"
        const val NOTIF_ID_REMINDER = 1001
        const val EXTRA_NOTIF_TYPE = "extra_notif_type"
        const val EXTRA_CAPSULE_ID = "extra_capsule_id"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_CAPSULE = "capsule"
    }
}

object NotificationHelper {

    fun scheduleDailyReminder(context: Context, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parts = timeStr.split(":").mapNotNull { it.toIntOrNull() }
        val hour = if (parts.isNotEmpty()) parts[0] else 20
        val minute = if (parts.size > 1) parts[1] else 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_REMINDER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, KannachAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {}
    }
}
