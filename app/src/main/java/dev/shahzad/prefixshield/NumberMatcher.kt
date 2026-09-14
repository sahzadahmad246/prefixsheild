package dev.shahzad.prefixshield

object NumberMatcher {
    fun digitsOnly(value: String): String = value.filter { it.isDigit() }

    fun extractNumber(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val core = raw.substringBefore("@").substringBefore(";")
        return digitsOnly(core)
    }

    fun normalizePrefix(prefix: String): String = digitsOnly(prefix)

    fun matchingPrefix(rawNumber: String?, prefixes: List<PrefixRule>): String? {
        val number = extractNumber(rawNumber)
        if (number.isEmpty()) return null
        val numberForms = variants(number, fullNumber = true)
        return prefixes
            .filter { it.enabled }
            .map { normalizePrefix(it.prefix) }
            .firstOrNull { prefix ->
                prefix.length >= 3 && matches(numberForms, prefix)
            }
    }

    private fun matches(numberForms: Set<String>, prefix: String): Boolean {
        val prefixForms = variants(prefix, fullNumber = false)
        return prefixForms.any { piece ->
            piece.length >= 3 && numberForms.any { number -> number.contains(piece) }
        }
    }

    /**
     * Builds forms with/without trunk 0, 00, and India +91 so a saved series
     * still matches whether the carrier sends +91, 91, 0, or a local 10-digit number.
     */
    internal fun variants(digits: String, fullNumber: Boolean): Set<String> {
        if (digits.isEmpty()) return emptySet()
        val out = linkedSetOf(digits)

        fun consider(value: String) {
            if (value.isNotEmpty()) out += value
        }

        if (digits.startsWith("00") && digits.length > 4) {
            consider(digits.drop(2))
        }
        if (digits.startsWith("0") && digits.length > 3) {
            consider(digits.drop(1))
        }

        val stripCountryCode = !fullNumber || digits.length >= 12
        if (stripCountryCode && digits.startsWith("91") && digits.length > 5) {
            consider(digits.drop(2))
        }

        out.toList().forEach { form ->
            if (form.startsWith("0") && form.length > 3) consider(form.drop(1))
            if ((!fullNumber || form.length >= 12) && form.startsWith("91") && form.length > 5) {
                consider(form.drop(2))
            }
        }
        return out
    }
}
