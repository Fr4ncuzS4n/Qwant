package com.example.qwant.data

import android.content.Context
import androidx.core.content.edit

/**
 * Локальная сессия водителя (сервер авторизации не имеет, водитель выбирается
 * из списка `GET /api/drivers`). Хранится в SharedPreferences.
 */
object SessionStore {

    private const val PREFS = "qwant_session"
    private const val KEY_DRIVER_ID = "driver_id"
    private const val KEY_DRIVER_NAME = "driver_name"
    private const val KEY_EQUIPMENT = "equipment_name"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveDriver(context: Context, id: Int, name: String, equipmentName: String?) {
        prefs(context).edit {
            putInt(KEY_DRIVER_ID, id)
            putString(KEY_DRIVER_NAME, name)
            putString(KEY_EQUIPMENT, equipmentName)
        }
    }

    fun driverId(context: Context): Int = prefs(context).getInt(KEY_DRIVER_ID, -1)

    fun driverName(context: Context): String =
        prefs(context).getString(KEY_DRIVER_NAME, "").orEmpty()

    fun equipmentName(context: Context): String? =
        prefs(context).getString(KEY_EQUIPMENT, null)

    fun isDriverSelected(context: Context): Boolean = driverId(context) > 0

    fun clearDriver(context: Context) {
        prefs(context).edit {
            remove(KEY_DRIVER_ID)
            remove(KEY_DRIVER_NAME)
            remove(KEY_EQUIPMENT)
        }
    }
}
