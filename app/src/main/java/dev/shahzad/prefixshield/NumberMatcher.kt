package dev.shahzad.prefixshield

object NumberMatcher {
    fun digitsOnly(value: String): String = buildString(value.length) {
        for (ch in value) {
            val digit = Character.digit(ch, 10)
            if (digit >= 0) append(digit)
        }
    }

    fun extractNumber(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val decoded = percentDecode(raw)
        val core = decoded.substringBefore("@").substringBefore(";")
            .substringAfter("tel:").substringAfter("TEL:")
        return digitsOnly(core)
    }

    /**
     * Number to hand to Telecom. 10-digit locals stay local.
     * Stored +91… keeps the plus. Bare 91 + 10 digits is dialed as 10 digits
     * so the network does not treat it as an invalid 12-digit local number.
     */
    fun dialable(raw: String): String {
        val decoded = percentDecode(raw)
        val core = decoded.substringBefore("@").substringBefore(";")
            .substringAfter("tel:").substringAfter("TEL:").trim()
        val special = core.filter { it.isDigit() || it == '+' || it == '*' || it == '#' || it == ',' || it == ';' }
        if (special.any { it == '*' || it == '#' }) return special.ifBlank { core }

        val hadPlus = core.startsWith("+")
        var digits = digitsOnly(core)
        if (digits.isEmpty()) return special

        if (digits.startsWith("00") && digits.length > 4) {
            return "+" + digits.drop(2)
        }
        if (hadPlus) return "+$digits"

        if (digits.startsWith("0") && digits.length == 11) {
            digits = digits.drop(1)
        }
        if (digits.length == 12 && digits.startsWith("91")) {
            return digits.takeLast(10)
        }
        if (digits.length > 12 && digits.startsWith("91") && digits.drop(2).length == 10) {
            return digits.takeLast(10)
        }
        return digits
    }

    fun normalizePrefix(prefix: String): String = extractNumber(prefix)

    fun matchingPrefix(rawNumber: String?, prefixes: List<PrefixRule>): String? =
        matchingPrefix(listOf(rawNumber), prefixes)

    fun matchingPrefix(rawNumbers: Collection<String?>, prefixes: List<PrefixRule>): String? {
        val enabled = prefixes
            .filter { it.enabled }
            .map { normalizePrefix(it.prefix) }
            .filter { it.length >= 3 }
        if (enabled.isEmpty()) return null

        val numberKeys = rawNumbers
            .map { extractNumber(it) }
            .filter { it.isNotEmpty() }
            .flatMap { keys(it, isPrefix = false) }
            .toSet()
        if (numberKeys.isEmpty()) return null

        return enabled.firstOrNull { prefix ->
            keys(prefix, isPrefix = true).any { piece ->
                piece.length >= 3 && numberKeys.any { number -> number.startsWith(piece) }
            }
        }
    }

    fun displayNumber(rawNumbers: Collection<String?>): String {
        return rawNumbers
            .map { extractNumber(it) }
            .filter { it.isNotEmpty() }
            .maxByOrNull { it.length }
            ?: "Unknown"
    }

    internal fun keys(digits: String, isPrefix: Boolean): Set<String> {
        if (digits.isEmpty()) return emptySet()
        val out = linkedSetOf(digits)

        fun add(value: String) {
            if (value.isNotEmpty()) out += value
        }

        out.toList().forEach { form ->
            var current = form
            if (current.startsWith("00") && current.length > 4) {
                current = current.drop(2)
                add(current)
            }
            if (current.startsWith("0") && current.length > 3) {
                add(current.drop(1))
            }
            val stripCc = isPrefix || current.length >= 12
            if (stripCc && current.startsWith("91") && current.length > 5) {
                add(current.drop(2))
            }
            if (!isPrefix && current.length > 10) {
                add(current.takeLast(10))
            }
        }

        // Second pass so last-10 / stripped forms also lose +91 / 0
        out.toList().forEach { form ->
            if (form.startsWith("0") && form.length > 3) add(form.drop(1))
            if ((isPrefix || form.length >= 12) && form.startsWith("91") && form.length > 5) {
                add(form.drop(2))
            }
            if (!isPrefix && form.length > 10) add(form.takeLast(10))
        }
        return out
    }

    private fun percentDecode(raw: String): String {
        var value = raw
        repeat(2) {
            value = value
                .replace("%2B", "+", ignoreCase = true)
                .replace("%2b", "+")
        }
        return value
    }
}
