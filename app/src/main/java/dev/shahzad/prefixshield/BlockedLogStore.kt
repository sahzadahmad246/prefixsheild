package dev.shahzad.prefixshield

data class BlockedCall(
    val number: String,
    val matchedPrefix: String,
    val atMillis: Long
)

class BlockedLogStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("blocked_log", android.content.Context.MODE_PRIVATE)

    fun list(): List<BlockedCall> {
        val raw = prefs.getString(KEY_LOG, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.lines()
            .mapNotNull { line ->
                val parts = line.split("|", limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val at = parts[0].toLongOrNull() ?: return@mapNotNull null
                BlockedCall(number = parts[1], matchedPrefix = parts[2], atMillis = at)
            }
    }

    fun add(call: BlockedCall) {
        val current = list()
        val duplicate = current.any {
            it.number == call.number && kotlin.math.abs(it.atMillis - call.atMillis) < 4000
        }
        if (duplicate) return
        val next = (current + call)
            .sortedByDescending { it.atMillis }
            .take(MAX_ITEMS)
        val serialized = next.joinToString("\n") { "${it.atMillis}|${it.number}|${it.matchedPrefix}" }
        prefs.edit()
            .putString(KEY_LOG, serialized)
            .putInt(KEY_COUNT, totalCount() + 1)
            .commit()
    }

    fun totalCount(): Int = prefs.getInt(KEY_COUNT, 0)

    fun clear() {
        prefs.edit().putString(KEY_LOG, "").commit()
    }

    fun register(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val KEY_LOG = "log"
        private const val KEY_COUNT = "count"
        private const val MAX_ITEMS = 200
    }
}
