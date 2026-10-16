package com.example.qwant.data

import android.content.Context
import androidx.core.content.edit

/**
 * Локальная сессия пользователя: токен и данные учётной записи
 * (`POST /api/auth/login`). Хранится в SharedPreferences.
 *
 * Токен подставляется в заголовок `X-Auth-Token` каждого запроса
 * (см. [QwantApi.login]/[QwantApi.logout] и [QwantApi.authToken]).
 */
object SessionStore {

    private const val PREFS = "qwant_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_LOGIN = "user_login"
    private const val KEY_NAME = "user_name"
    private const val KEY_ROLE = "user_role"
    private const val KEY_DRIVER_ID = "driver_id"
    private const val KEY_DRIVER_NAME = "driver_name"
    private const val KEY_EQUIPMENT = "equipment_name"
    private const val KEY_EQUIPMENT_TYPE = "equipment_type"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Сохранить сессию после успешного входа. */
    fun save(context: Context, response: LoginResponse) {
        val user = response.user
        prefs(context).edit {
            putString(KEY_TOKEN, response.token)
            putInt(KEY_USER_ID, user.id)
            putString(KEY_LOGIN, user.login)
            putString(KEY_NAME, user.name)
            putString(KEY_ROLE, user.role)
            putInt(KEY_DRIVER_ID, user.driverId ?: -1)
            putString(KEY_DRIVER_NAME, user.driverName)
            putString(KEY_EQUIPMENT, user.equipmentName)
            putString(KEY_EQUIPMENT_TYPE, user.equipmentType)
        }
    }

    /** Сессия или null, если токена нет (пользователь не вошёл). */
    fun session(context: Context): UserSession? {
        val p = prefs(context)
        val token = p.getString(KEY_TOKEN, null) ?: return null
        val role = p.getString(KEY_ROLE, null) ?: return null
        return UserSession(
            token = token,
            userId = p.getInt(KEY_USER_ID, -1),
            login = p.getString(KEY_LOGIN, "").orEmpty(),
            name = p.getString(KEY_NAME, "").orEmpty(),
            role = role,
            driverId = p.getInt(KEY_DRIVER_ID, -1).takeIf { it > 0 },
            driverName = p.getString(KEY_DRIVER_NAME, null),
            equipmentName = p.getString(KEY_EQUIPMENT, null),
            equipmentType = p.getString(KEY_EQUIPMENT_TYPE, null),
        )
    }

    fun isLoggedIn(context: Context): Boolean = session(context) != null

    fun clear(context: Context) {
        prefs(context).edit { clear() }
    }
}

/** Сессия пользователя, прочитанная из SharedPreferences. */
data class UserSession(
    val token: String,
    val userId: Int,
    val login: String,
    val name: String,
    val role: String,
    val driverId: Int? = null,
    val driverName: String? = null,
    val equipmentName: String? = null,
    val equipmentType: String? = null,
) {
    val isDispatcher: Boolean get() = role == Role.DISPATCHER
    val isDriver: Boolean get() = role == Role.DRIVER
}
