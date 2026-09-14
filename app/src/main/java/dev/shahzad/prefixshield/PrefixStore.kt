package dev.shahzad.prefixshield

data class PrefixRule(
    val prefix: String,
    val enabled: Boolean = true
)

class PrefixStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("prefix_rules", android.content.Context.MODE_PRIVATE)

    fun list(): List<PrefixRule> {
        val raw = prefs.getString(KEY_RULES, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val enabled = !line.startsWith("!")
                val prefix = line.removePrefix("!").trim()
                PrefixRule(prefix = prefix, enabled = enabled)
            }
            .filter { it.prefix.isNotEmpty() }
    }

    fun save(rules: List<PrefixRule>) {
        val serialized = rules.joinToString("\n") { rule ->
            if (rule.enabled) rule.prefix else "!${rule.prefix}"
        }
        prefs.edit().putString(KEY_RULES, serialized).commit()
    }

    fun add(prefix: String): List<PrefixRule> {
        val normalized = NumberMatcher.normalizePrefix(prefix)
        require(normalized.length >= 3) { "Prefix is too short" }
        val current = list().toMutableList()
        if (current.any { NumberMatcher.normalizePrefix(it.prefix) == normalized }) {
            return current
        }
        current.add(0, PrefixRule(prefix = normalized, enabled = true))
        save(current)
        return current
    }

    fun remove(prefix: String): List<PrefixRule> {
        val normalized = NumberMatcher.normalizePrefix(prefix)
        val updated = list().filterNot { NumberMatcher.normalizePrefix(it.prefix) == normalized }
        save(updated)
        return updated
    }

    fun toggle(prefix: String, enabled: Boolean): List<PrefixRule> {
        val normalized = NumberMatcher.normalizePrefix(prefix)
        val updated = list().map {
            if (NumberMatcher.normalizePrefix(it.prefix) == normalized) it.copy(enabled = enabled) else it
        }
        save(updated)
        return updated
    }

    companion object {
        private const val KEY_RULES = "rules"
    }
}
