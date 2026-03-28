package com.htmlwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedUri: Uri? = null
    private var selectedFileName = ""

    private lateinit var tvFile: TextView
    private lateinit var btnSelectFile: Button
    private lateinit var rgSize: RadioGroup
    private lateinit var spinnerInterval: Spinner
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Widget ID al — bulamazsa çık
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // İptal sonucu olarak ayarla (kullanıcı geri basarsa)
        setResult(RESULT_CANCELED, Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        })

        setContentView(R.layout.activity_widget_config)

        tvFile = findViewById(R.id.tv_selected_file)
        btnSelectFile = findViewById(R.id.btn_select_file)
        rgSize = findViewById(R.id.rg_size)
        spinnerInterval = findViewById(R.id.spinner_interval)
        btnAdd = findViewById(R.id.btn_add)
        btnCancel = findViewById(R.id.btn_cancel)

        // Yenileme süresi spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Prefs.INTERVAL_LABELS)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = adapter
        spinnerInterval.setSelection(2) // Default: 15 dakika

        btnSelectFile.setOnClickListener { openFilePicker() }

        btnCancel.setOnClickListener { finish() }

        btnAdd.setOnClickListener { saveAndFinish() }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/html"
            // HTML uzantılarını da kabul et
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/html", "text/htm", "application/xhtml+xml", "*/*"))
        }
        startActivityForResult(intent, REQUEST_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            // Kalıcı okuma izni al
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Bazı dosyalar persistable izne izin vermez, sorun değil
            }

            selectedUri = uri
            selectedFileName = getFileName(uri)
            tvFile.text = selectedFileName
        }
    }

    private fun getFileName(uri: Uri): String {
        // Content URI'dan dosya adı al
        var name = ""
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
        } catch (e: Exception) { }

        if (name.isEmpty()) name = uri.lastPathSegment ?: "dosya.html"
        return name
    }

    private fun saveAndFinish() {
        if (selectedUri == null) {
            Toast.makeText(this, "Lütfen bir HTML dosyası seçin", Toast.LENGTH_SHORT).show()
            return
        }

        // Boyut
        val sizeIndex = when (rgSize.checkedRadioButtonId) {
            R.id.rb_small -> 0
            R.id.rb_large -> 2
            else -> 1
        }

        // Yenileme
        val intervalIndex = spinnerInterval.selectedItemPosition

        // Kaydet
        Prefs.setFileUri(this, widgetId, selectedUri.toString())
        Prefs.setFileName(this, widgetId, selectedFileName)
        Prefs.setSizeIndex(this, widgetId, sizeIndex)
        Prefs.setIntervalIndex(this, widgetId, intervalIndex)

        // Widget'ı güncelle
        val mgr = AppWidgetManager.getInstance(this)
        HtmlWidgetProvider.update(this, mgr, widgetId)
        HtmlWidgetProvider.scheduleAlarm(this, widgetId)

        // Başarılı sonuç döndür
        setResult(RESULT_OK, Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        })
        finish()
    }

    companion object {
        private const val REQUEST_FILE = 1001
    }
}
