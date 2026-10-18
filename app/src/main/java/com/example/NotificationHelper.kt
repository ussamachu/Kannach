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

                val settings = repo.getUserSettings()
                val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                if (settings.reminderDays.contains(todayDow)) {
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
                }

                // Reschedule for next enabled day
                NotificationHelper.scheduleDailyReminder(context, reminder.time)
            }

            TYPE_CAPSULE -> {
                val capsuleTitle = strings.capsuleTitle
                val capsuleSub = strings.capsuleReadyBanner

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

            TYPE_COPY_REMINDER -> {
                val settings = repo.getUserSettings()
                if (settings.copyReminder != "monthly") return

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(strings.appTitle)
                    .setContentText(strings.copyReminderNotifMsg)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(strings.copyReminderNotifMsg))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(NOTIF_ID_COPY_REMINDER, notif)
                NotificationHelper.scheduleMonthlyCopyReminder(context)
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
        const val NOTIF_ID_TEST_NUDGE = 1002
        const val NOTIF_ID_COPY_REMINDER = 1003
        const val EXTRA_NOTIF_TYPE = "extra_notif_type"
        const val EXTRA_CAPSULE_ID = "extra_capsule_id"
        const val EXTRA_CAPSULE_STRING_ID = "extra_capsule_string_id"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_CAPSULE = "capsule"
        const val TYPE_COPY_REMINDER = "copy_reminder"
    }
}

object NotificationHelper {

    fun capsuleRequestCode(capsuleId: String): Int {
        return 2000 + (capsuleId.hashCode() and 0x0FFFFFFF)
    }

    /**
     * Immediately sends a test nudge notification using one of the 7 rotating messages
     * and returns the chosen message text.
     */
    fun sendTestNudge(context: Context): String {
        val repo = KannachRepository(context)
        val lang = repo.getLanguage()
        val strings = I18n.forLanguage(lang)
        val reminder = repo.getReminder()
        val msgs = strings.reminderMsgs
        var idx = Random.nextInt(msgs.size)
        if (msgs.size > 1 && idx == reminder.lastIdx) {
            idx = (idx + 1) % msgs.size
        }
        repo.updateReminderLastIdx(idx)
        val message = msgs[idx]

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    KannachAlarmReceiver.CHANNEL_ID,
                    "Kannach Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.createNotificationChannel(channel)
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(context, KannachAlarmReceiver.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(strings.appTitle)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(KannachAlarmReceiver.NOTIF_ID_TEST_NUDGE, notif)
        } catch (_: Exception) {}

        return message
    }

    fun scheduleDailyReminder(context: Context, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val repo = KannachRepository(context)
        val enabledDays = repo.getUserSettings().reminderDays
        if (enabledDays.isEmpty()) {
            cancelDailyReminder(context)
            return
        }

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
            var guard = 0
            while (!enabledDays.contains(get(Calendar.DAY_OF_WEEK)) && guard < 7) {
                add(Calendar.DAY_OF_YEAR, 1)
                guard++
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
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    fun scheduleMonthlyCopyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 30)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_COPY_REMINDER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_COPY_REMINDER,
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

    fun cancelMonthlyCopyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_COPY_REMINDER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_COPY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    /**
     * CP9 & RB1: Schedules a local notification for [capsule] at 09:00 AM on its [TimeCapsule.unlockDate].
     */
    fun scheduleCapsuleNotification(context: Context, capsule: TimeCapsule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parts = capsule.unlockDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0])
            set(Calendar.MONTH, parts[1] - 1)
            set(Calendar.DAY_OF_MONTH, parts[2])
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reqCode = capsuleRequestCode(capsule.id)
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_CAPSULE)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_ID, reqCode)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_STRING_ID, capsule.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
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

    /**
     * CP9: Cancels a scheduled unlock notification when a capsule is opened or deleted.
     */
    fun cancelCapsuleNotification(context: Context, capsuleId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reqCode = capsuleRequestCode(capsuleId)
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_CAPSULE)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_ID, reqCode)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_STRING_ID, capsuleId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    /**
     * Checks whether a capsule unlock PendingIntent is currently registered.
     */
    fun isCapsuleNotificationScheduled(context: Context, capsuleId: String): Boolean {
        val reqCode = capsuleRequestCode(capsuleId)
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_CAPSULE)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_ID, reqCode)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_STRING_ID, capsuleId)
        }
        val existing = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return existing != null
    }

    /**
     * Checks whether the daily reminder PendingIntent is currently registered.
     */
    fun isDailyReminderScheduled(context: Context): Boolean {
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_REMINDER)
        }
        val existing = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_REMINDER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return existing != null
    }
}

