package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import java.util.Calendar
import kotlin.math.roundToInt

abstract class KannachWidgetProvider : AppWidgetProvider() {

    protected open val defaultWidthDp: Int = 250
    protected open val defaultHeightDp: Int = 250
    protected open val openTodayOnClick: Boolean = true

    override fun onEnabled(context: Context) {
        KannachWidgets.scheduleDailyRefresh(context)
    }

    override fun onDisabled(context: Context) {
        if (!KannachWidgets.hasAnyWidget(context)) {
            KannachWidgets.cancelDailyRefresh(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            refresh(context, appWidgetManager, id)
        }
        KannachWidgets.scheduleDailyRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        refresh(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            "android.intent.action.DATE_CHANGED",
            "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED" -> KannachWidgets.updateAll(context)
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.MY_PACKAGE_REPLACED" -> {
                KannachWidgets.updateAll(context)
                KannachWidgets.scheduleDailyRefresh(context)
            }
        }
    }

    abstract fun render(context: Context, widthPx: Int, heightPx: Int): Bitmap

    fun refresh(context: Context, manager: AppWidgetManager, id: Int) {
        val options = manager.getAppWidgetOptions(id)
        val isLandscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val widthDp = (if (isLandscape) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, defaultWidthDp)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultWidthDp)
        }).let { if (it > 0) it else defaultWidthDp }

        val heightDp = (if (isLandscape) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeightDp)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, defaultHeightDp)
        }).let { if (it > 0) it else defaultHeightDp }

        val density = context.resources.displayMetrics.density
        val rawWidthPx = (widthDp * density).roundToInt().coerceAtLeast(100)
        val rawHeightPx = (heightDp * density).roundToInt().coerceAtLeast(100)
        val (widthPx, heightPx) = WidgetRenderer.clampDimensions(rawWidthPx, rawHeightPx)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            if (openTodayOnClick) {
                action = "com.ossamah.kannach.OPEN_TODAY"
            } else {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val initialFallback = WidgetRenderer.renderFallback(context, widthPx, heightPx)
        val initialViews = RemoteViews(context.packageName, R.layout.widget_image).apply {
            setImageViewBitmap(R.id.widget_image, initialFallback)
            setOnClickPendingIntent(R.id.widget_image, pendingIntent)
        }
        manager.updateAppWidget(id, initialViews)

        try {
            val bitmap = render(context, widthPx, heightPx)
            val views = RemoteViews(context.packageName, R.layout.widget_image).apply {
                setImageViewBitmap(R.id.widget_image, bitmap)
                setOnClickPendingIntent(R.id.widget_image, pendingIntent)
            }
            manager.updateAppWidget(id, views)
        } catch (t: Throwable) {
            Log.e("KannachWidget", "render failed", t)
        }
    }
}

class YearWidgetProvider : KannachWidgetProvider() {
    override val defaultWidthDp: Int = 250
    override val defaultHeightDp: Int = 250
    override val openTodayOnClick: Boolean = false

    override fun render(context: Context, widthPx: Int, heightPx: Int): Bitmap =
        WidgetRenderer.renderYear(context, widthPx, heightPx)
}

class TodayWidgetProvider : KannachWidgetProvider() {
    override val defaultWidthDp: Int = 250
    override val defaultHeightDp: Int = 110
    override val openTodayOnClick: Boolean = true

    override fun render(context: Context, widthPx: Int, heightPx: Int): Bitmap =
        WidgetRenderer.renderToday(context, widthPx, heightPx, compact = false)
}

class SmallWidgetProvider : KannachWidgetProvider() {
    override val defaultWidthDp: Int = 110
    override val defaultHeightDp: Int = 110
    override val openTodayOnClick: Boolean = true

    override fun render(context: Context, widthPx: Int, heightPx: Int): Bitmap =
        WidgetRenderer.renderToday(context, widthPx, heightPx, compact = true)
}

object KannachWidgets {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val providers = listOf(
            YearWidgetProvider(),
            TodayWidgetProvider(),
            SmallWidgetProvider()
        )
        for (provider in providers) {
            try {
                val ids = manager.getAppWidgetIds(ComponentName(context, provider::class.java))
                for (id in ids) {
                    try {
                        provider.refresh(context, manager, id)
                    } catch (t: Throwable) {
                        Log.e("KannachWidget", "refresh failed", t)
                    }
                }
            } catch (t: Throwable) {
                Log.e("KannachWidget", "updateAll failed", t)
            }
        }
    }

    fun scheduleDailyRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 20)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val intent = Intent(context, WidgetDayChangeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            7311,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, cal.timeInMillis, pendingIntent)
        } catch (_: SecurityException) {
        }
    }

    fun cancelDailyRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WidgetDayChangeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            7311,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun hasAnyWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val classes = listOf(
            YearWidgetProvider::class.java,
            TodayWidgetProvider::class.java,
            SmallWidgetProvider::class.java
        )
        return classes.any { cls ->
            try {
                manager.getAppWidgetIds(ComponentName(context, cls)).isNotEmpty()
            } catch (_: Throwable) {
                false
            }
        }
    }
}

class WidgetDayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            KannachWidgets.updateAll(context)
            if (KannachWidgets.hasAnyWidget(context)) {
                KannachWidgets.scheduleDailyRefresh(context)
            }
        } catch (t: Throwable) {
            Log.e("KannachWidget", "day change refresh failed", t)
        }
    }
}
