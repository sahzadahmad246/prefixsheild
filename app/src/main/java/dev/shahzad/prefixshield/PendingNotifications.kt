package dev.shahzad.prefixshield

import android.content.Context

class PendingNotifications(context: Context) {
    private val prefs = context.getSharedPreferences("pending_notes", Context.MODE_PRIVATE)

    fun add(number: String) {
        val next = (list() + number).distinct().takeLast(7)
        prefs.edit().putString(KEY, next.joinToString("\n")).commit()
    }

    fun list(): List<String> {
        val raw = prefs.getString(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.lines().filter { it.isNotBlank() }
    }

    fun clear() {
        prefs.edit().putString(KEY, "").commit()
    }

    companion object {
        private const val KEY = "numbers"
    }
}
