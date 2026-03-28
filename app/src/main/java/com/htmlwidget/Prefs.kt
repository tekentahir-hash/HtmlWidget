package com.htmlwidget

import android.content.Context

object Prefs {

    private const val NAME = "html_widget_prefs"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // Yenileme seçenekleri
    val INTERVAL_LABELS = arrayOf("1 Dakika", "5 Dakika", "15 Dakika", "30 Dakika", "1 Saat", "6 Saat")
    val INTERVAL_MS = longArrayOf(60_000L, 300_000L, 900_000L, 1_800_000L, 3_600_000L, 21_600_000L)

    // Boyut seçenekleri
    val SIZE_LABELS = arrayOf("Küçük (2×2)", "Orta (3×3)", "Büyük (4×4)")
    val SIZE_DP = intArrayOf(148, 222, 296) // Yaklaşık piksel boyutu dp cinsinden

    fun setFileUri(ctx: Context, id: Int, uri: String) =
        sp(ctx).edit().putString("uri_$id", uri).apply()

    fun getFileUri(ctx: Context, id: Int): String? =
        sp(ctx).getString("uri_$id", null)

    fun setFileName(ctx: Context, id: Int, name: String) =
        sp(ctx).edit().putString("name_$id", name).apply()

    fun getFileName(ctx: Context, id: Int): String =
        sp(ctx).getString("name_$id", "dosya.html") ?: "dosya.html"

    fun setIntervalIndex(ctx: Context, id: Int, index: Int) =
        sp(ctx).edit().putInt("interval_$id", index).apply()

    fun getIntervalIndex(ctx: Context, id: Int): Int =
        sp(ctx).getInt("interval_$id", 2) // default: 15 dakika

    fun getIntervalMs(ctx: Context, id: Int): Long =
        INTERVAL_MS[getIntervalIndex(ctx, id).coerceIn(0, INTERVAL_MS.size - 1)]

    fun setSizeIndex(ctx: Context, id: Int, index: Int) =
        sp(ctx).edit().putInt("size_$id", index).apply()

    fun getSizeIndex(ctx: Context, id: Int): Int =
        sp(ctx).getInt("size_$id", 1) // default: orta

    fun removeWidget(ctx: Context, id: Int) {
        sp(ctx).edit()
            .remove("uri_$id")
            .remove("name_$id")
            .remove("interval_$id")
            .remove("size_$id")
            .apply()
    }

    fun getAllWidgetIds(ctx: Context): List<Int> {
        val all = sp(ctx).all
        return all.keys
            .filter { it.startsWith("uri_") }
            .mapNotNull { it.removePrefix("uri_").toIntOrNull() }
    }
}
