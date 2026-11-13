package com.example.sustainscore.data

import android.content.Context
import com.example.sustainscore.model.ActivityLog
import org.json.JSONArray
import org.json.JSONObject

/** Keeps presenter edits across app restarts, without an account or backend. */
class DemoLogStore(context: Context) {
    private val preferences = context.getSharedPreferences("sustainshare_demo", Context.MODE_PRIVATE)

    fun loadLogs(): List<ActivityLog>? {
        val raw = preferences.getString("logs", null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                ActivityLog(item.getString("id"), item.getString("type"), item.getLong("start"),
                    item.getDouble("quantity"), item.getInt("duration"))
            }
        }.getOrNull()
    }

    fun saveLogs(logs: List<ActivityLog>) {
        val array = JSONArray()
        logs.forEach { log ->
            array.put(JSONObject().put("id", log.id).put("type", log.activityTypeId)
                .put("start", log.startTimestampMillis).put("quantity", log.quantity)
                .put("duration", log.durationMinutes))
        }
        preferences.edit().putString("logs", array.toString()).apply()
    }

    var predictionEnabled: Boolean
        get() = preferences.getBoolean("prediction_enabled", false)
        set(value) { preferences.edit().putBoolean("prediction_enabled", value).apply() }
}
