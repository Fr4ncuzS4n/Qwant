package com.example.qwant.data

import android.content.Context
import androidx.core.content.edit

/**
 * Адрес сервера, настраиваемый прямо в приложении (экран выбора кабинета).
 *
 * Дефолт — `10.0.2.2:8080` (host-машина из Android-эмулятора).
 * Если 8080 на машине занят или сервер поднят на другом порту/адресе —
 * адрес можно поменять без пересборки APK.
 */
object ServerConfig {

    /** Порт и адрес по умолчанию — совпадают с дефолтом сервера (`server/`). */
    const val DEFAULT_URL = "http://10.0.2.2:8080"

    private const val PREFS = "qwant_server"
    private const val KEY_URL = "base_url"

    @Volatile
    var baseUrl: String = DEFAULT_URL
        private set

    fun init(context: Context) {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, null)
        baseUrl = normalize(stored ?: DEFAULT_URL)
    }

    fun save(context: Context, url: String) {
        val normalized = normalize(url)
        baseUrl = normalized
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_URL, normalized)
        }
    }

    /** Убирает пробелы/хвостовой слэш, добавляет схему, если её забыли. */
    private fun normalize(url: String): String {
        var result = url.trim().trimEnd('/')
        if (result.isEmpty()) return DEFAULT_URL
        if (!result.startsWith("http://") && !result.startsWith("https://")) {
            result = "http://$result"
        }
        return result
    }
}
