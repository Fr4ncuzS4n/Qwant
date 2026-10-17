package com.example.qwant.data

import android.content.Context
import androidx.core.content.edit

/**
 * Адрес сервера (`server/`). По умолчанию — `http://10.0.2.2:8080`
 * (хост-машина из Android-эмулятора). Настраивается в приложении на экране
 * входа и сохраняется в SharedPreferences — без пересборки APK.
 */
object ServerConfig {

    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

    private const val PREFS = "qwant_server"
    private const val KEY_URL = "base_url"

    /** Текущий адрес сервера; читается [QwantApi] перед каждым запросом. */
    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    /** Вызывается из `MainActivity.onCreate` до первого запроса API. */
    fun init(context: Context) {
        baseUrl = normalize(prefs(context).getString(KEY_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL)
    }

    /** Сохранить адрес (без завершающего «/»). */
    fun save(context: Context, url: String) {
        val normalized = normalize(url)
        prefs(context).edit { putString(KEY_URL, normalized) }
        baseUrl = normalized
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun normalize(url: String): String = url.trim().trimEnd('/')
}
