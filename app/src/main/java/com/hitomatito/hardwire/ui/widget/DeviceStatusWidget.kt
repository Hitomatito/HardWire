package com.hitomatito.hardwire.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.hitomatito.hardwire.R

class DeviceStatusWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_device_status)
            
            // Load device info from shared preferences
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val deviceName = prefs.getString("device_name", "Sin dispositivo") ?: "Sin dispositivo"
            val batteryLevel = prefs.getString("battery_level", "--") ?: "--"
            val connectionStatus = prefs.getString("connection_status", "Desconectado") ?: "Desconectado"
            
            views.setTextViewText(R.id.widget_device_name, deviceName)
            views.setTextViewText(R.id.widget_battery, "Bateria: $batteryLevel%")
            views.setTextViewText(R.id.widget_status, connectionStatus)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
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
        }
    }
}