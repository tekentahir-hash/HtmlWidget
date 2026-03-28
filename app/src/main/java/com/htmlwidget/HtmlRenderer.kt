package com.htmlwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

object HtmlRenderer {

    fun render(
        ctx: Context,
        uri: String,
        widthPx: Int,
        heightPx: Int,
        onDone: (Bitmap?) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            try {
                val wv = WebView(ctx)
                wv.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(heightPx, android.view.View.MeasureSpec.EXACTLY)
                )
                wv.layout(0, 0, widthPx, heightPx)
                wv.setBackgroundColor(Color.WHITE)

                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true
                    allowContentAccess = true
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bmp)
                                canvas.drawColor(Color.WHITE)
                                wv.draw(canvas)
                                wv.destroy()
                                onDone(bmp)
                            } catch (e: Exception) {
                                onDone(null)
                            }
                        }, 900)
                    }

                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        wv.destroy()
                        onDone(null)
                    }
                }

                // URI'dan içeriği oku ve yükle
                try {
                    val parsedUri = Uri.parse(uri)
                    val stream = ctx.contentResolver.openInputStream(parsedUri)
                    val html = stream?.bufferedReader()?.readText() ?: ""
                    stream?.close()

                    // Base URL: dosyanın bulunduğu klasör (CSS/JS için)
                    val basePath = parsedUri.path?.let {
                        val parent = java.io.File(it).parent
                        if (parent != null) "file://$parent/" else null
                    }
                    wv.loadDataWithBaseURL(basePath, html, "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    onDone(null)
                }

            } catch (e: Exception) {
                onDone(null)
            }
        }
    }

    /** Boyut indeksine göre widget piksel boyutunu hesapla */
    fun sizePx(ctx: Context, sizeIndex: Int): Int {
        val density = ctx.resources.displayMetrics.density
        val dp = Prefs.SIZE_DP[sizeIndex.coerceIn(0, 2)]
        return (dp * density).toInt().coerceAtLeast(200)
    }
}
