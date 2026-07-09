package com.rk.terminal.ui.screens.terminal

import com.rk.settings.Preference
import com.rk.settings.Settings
import com.rk.terminal.ui.screens.settings.WorkingMode
import org.json.JSONArray
import org.json.JSONObject

data class CustomSession(
    val id: String,
    val name: String,
    val shellPath: String
)

object CustomSessions {
    private const val KEY = "custom_sessions"
    private const val DEFAULT_ID_KEY = "default_custom_session_id"

    fun getAll(): List<CustomSession> {
        val raw = Preference.getString(key = KEY, default = "[]")
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CustomSession(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    shellPath = obj.getString("shellPath")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(name: String, shellPath: String) {
        val list = getAll().toMutableList()
        list.add(CustomSession(id = System.currentTimeMillis().toString(), name = name, shellPath = shellPath))
        save(list)
    }

    fun remove(id: String) {
        val list = getAll().filterNot { it.id == id }
        save(list)
        if (getDefaultId() == id) {
            clearDefault()
        }
    }

    fun getById(id: String): CustomSession? = getAll().firstOrNull { it.id == id }

    fun getDefaultId(): String? {
        val id = Preference.getString(key = DEFAULT_ID_KEY, default = "")
        return id.ifBlank { null }
    }

    fun setDefault(id: String) {
        Preference.setString(key = DEFAULT_ID_KEY, value = id)
    }

    fun clearDefault() {
        Preference.setString(key = DEFAULT_ID_KEY, value = "")
    }

    fun resolveDefaultSession(): Pair<Int, CustomSession?> {
        return if (Settings.default_is_custom) {
            val id = getDefaultId()
            val session = id?.let { getById(it) }
            if (session != null) {
                WorkingMode.ALPINE to session
            } else {
                Settings.default_is_custom = false
                Settings.working_Mode to null
            }
        } else {
            Settings.working_Mode to null
        }
    }

    private fun save(list: List<CustomSession>) {
        val arr = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("shellPath", it.shellPath)
            arr.put(obj)
        }
        Preference.setString(key = KEY, value = arr.toString())
    }
}
