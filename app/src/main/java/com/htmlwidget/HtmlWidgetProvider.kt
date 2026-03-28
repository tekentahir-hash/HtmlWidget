package com.htmlwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class HtmlWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.htmlwidget.ACTION_REFRESH"
        const val EXTRA_ID = "widget_id"

        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(ctx.packageName, R.layout.widget_layout)

            val uri = Prefs.getFileUri(ctx, id)
            val fileName = Prefs.getFileName(ctx, id)

            // Alt şerit bilgisi
            views.setTextViewText(R.id.tv_filename, fileName)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            views.setTextViewText(R.id.tv_time, time)

            // Dokunca yenile
            val intent = Intent(ctx, HtmlWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(EXTRA_ID, id)
            }
            val pi = PendingIntent.getBroadcast(
                ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.iv_content, pi)
            views.setOnClickPendingIntent(R.id.ll_empty, pi)

            if (uri == null) {
                // Dosya seçilmemiş
                views.setViewVisibility(R.id.iv_content, View.GONE)
                views.setViewVisibility(R.id.ll_empty, View.VISIBLE)
                views.setTextViewText(R.id.tv_status, ctx.getString(R.string.no_file))
                mgr.updateAppWidget(id, views)
                return
            }

            // Yükleniyor göster
            views.setViewVisibility(R.id.ll_empty, View.VISIBLE)
            views.setViewVisibility(R.id.iv_content, View.GONE)
            views.setTextViewText(R.id.tv_status, ctx.getString(R.string.loading))
            mgr.updateAppWidget(id, views)

            // Render et
            val sizePx = HtmlRenderer.sizePx(ctx, Prefs.getSizeIndex(ctx, id))
            HtmlRenderer.render(ctx, uri, sizePx, sizePx) { bmp ->
                val v2 = RemoteViews(ctx.packageName, R.layout.widget_layout)
                v2.setTextViewText(R.id.tv_filename, fileName)
                val t2 = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                v2.setTextViewText(R.id.tv_time, t2)
                v2.setOnClickPendingIntent(R.id.iv_content, pi)
                v2.setOnClickPendingIntent(R.id.ll_empty, pi)

                if (bmp != null) {
                    v2.setImageViewBitmap(R.id.iv_content, bmp)
                    v2.setViewVisibility(R.id.iv_content, View.VISIBLE)
                    v2.setViewVisibility(R.id.ll_empty, View.GONE)
                } else {
                    v2.setViewVisibility(R.id.iv_content, View.GONE)
                    v2.setViewVisibility(R.id.ll_empty, View.VISIBLE)
                    v2.setTextViewText(R.id.tv_status, ctx.getString(R.string.file_error))
                }
                mgr.updateAppWidget(id, v2)
            }
        }

        fun scheduleAlarm(ctx: Context, id: Int) {
            val intervalMs = Prefs.getIntervalMs(ctx, id)
            val intent = Intent(ctx, HtmlWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(EXTRA_ID, id)
            }
            val pi = PendingIntent.getBroadcast(
                ctx, id + 10000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pi)
            am.setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + intervalMs,
                intervalMs,
                pi
            )
        }

        fun cancelAlarm(ctx: Context, id: Int) {
            val intent = Intent(ctx, HtmlWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(EXTRA_ID, id)
            }
            val pi = PendingIntent.getBroadcast(
                ctx, id + 10000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pi)
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            update(ctx, mgr, id)
            scheduleAlarm(ctx, id)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) {
            val id = intent.getIntExtra(EXTRA_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                update(ctx, AppWidgetManager.getInstance(ctx), id)
            }
        }
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ids.forEach { id ->
            cancelAlarm(ctx, id)
            Prefs.removeWidget(ctx, id)
        }
    }
}
