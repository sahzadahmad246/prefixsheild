package dev.shahzad.prefixshield

object ContactVCard {
    fun export(contacts: List<DeviceContact>): String = buildString {
        contacts.forEach { contact ->
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("N:${escape(contact.familyName)};${escape(contact.givenName)};;;")
            appendLine("FN:${escape(contact.name)}")
            if (contact.company.isNotBlank()) appendLine("ORG:${escape(contact.company)}")
            contact.numbers.forEach { number ->
                if (number.isNotBlank()) appendLine("TEL;TYPE=CELL:${escape(number)}")
            }
            if (contact.email.isNotBlank()) appendLine("EMAIL:${escape(contact.email)}")
            if (contact.note.isNotBlank()) appendLine("NOTE:${escape(contact.note)}")
            appendLine("END:VCARD")
        }
    }

    fun parse(text: String): List<ContactDraft> {
        val cards = text.split(Regex("BEGIN:VCARD", RegexOption.IGNORE_CASE)).drop(1)
        return cards.mapNotNull { card ->
            var given = ""
            var family = ""
            var fn = ""
            var org = ""
            var email = ""
            var note = ""
            val phones = mutableListOf<String>()
            card.lineSequence().forEach { raw ->
                val line = raw.trim().removePrefix("item1.").removePrefix("item2.")
                val key = line.substringBefore(":").substringBefore(";").uppercase()
                val value = unescape(line.substringAfter(":", ""))
                when (key) {
                    "FN" -> fn = value
                    "N" -> {
                        val parts = value.split(";")
                        family = parts.getOrNull(0).orEmpty()
                        given = parts.getOrNull(1).orEmpty()
                    }
                    "ORG" -> org = value.substringBefore(";")
                    "EMAIL" -> if (email.isBlank()) email = value
                    "NOTE" -> note = value
                    "TEL" -> if (value.isNotBlank()) phones += value
                }
            }
            val phone = phones.firstOrNull { it.isNotBlank() } ?: return@mapNotNull null
            if (given.isBlank() && family.isBlank() && fn.isNotBlank()) {
                val parts = fn.split(" ", limit = 2)
                given = parts[0]
                family = parts.getOrNull(1).orEmpty()
            }
            ContactDraft(
                givenName = given,
                familyName = family,
                company = org,
                phone = phone,
                email = email,
                note = note,
                account = ContactAccount("", ContactAccount.TYPE_PHONE, "Phone")
            )
        }
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;")

    private fun unescape(value: String) = value.replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")
}
