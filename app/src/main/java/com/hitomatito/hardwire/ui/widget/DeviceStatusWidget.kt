package com.hitomatito.hardwire.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hitomatito.hardwire.R
import com.hitomatito.hardwire.ui.MainActivity

class DeviceStatusWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_device_status)

        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val deviceName = prefs.getString("device_name", null)
            ?: context.getString(R.string.drawer_no_devices)
        val batteryLevel = prefs.getString("battery_level", "--") ?: "--"
        val connectionStatus = prefs.getString("connection_status", null)
            ?: context.getString(R.string.disconnect)

        views.setTextViewText(R.id.widget_device_name, deviceName)
        views.setTextViewText(R.id.widget_battery, context.getString(R.string.widget_battery_format, batteryLevel))
        views.setTextViewText(R.id.widget_status, connectionStatus)

        // Tap opens the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_device_name, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun updateWidget(
            context: Context,
            deviceName: String,
            batteryLevel: String,
            connectionStatus: String
        ) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("device_name", deviceName)
                .putString("battery_level", batteryLevel)
                .putString("connection_status", connectionStatus)
                .apply()

            // Trigger AppWidgetManager update for all widget instances
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, DeviceStatusWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            for (widgetId in widgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_device_status)
                views.setTextViewText(R.id.widget_device_name, deviceName)
                views.setTextViewText(R.id.widget_battery, context.getString(R.string.widget_battery_format, batteryLevel))
                views.setTextViewText(R.id.widget_status, connectionStatus)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
