package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import kotlin.math.roundToInt

abstract class KannachWidgetProvider : AppWidgetProvider() {

    protected open val defaultWidthDp: Int = 250
    protected open val defaultHeightDp: Int = 250
    protected open val openTodayOnClick: Boolean = true

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            refresh(context, appWidgetManager, id)
        }
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
        val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
        val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)

        val bitmap = try {
            render(context, widthPx, heightPx)
        } catch (_: Throwable) {
            return
        }

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

        val views = RemoteViews(context.packageName, R.layout.widget_image).apply {
            setImageViewBitmap(R.id.widget_image, bitmap)
            setOnClickPendingIntent(R.id.widget_image, pendingIntent)
        }

        manager.updateAppWidget(id, views)
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
            val ids = manager.getAppWidgetIds(ComponentName(context, provider::class.java))
            for (id in ids) {
                provider.refresh(context, manager, id)
            }
        }
    }
}
