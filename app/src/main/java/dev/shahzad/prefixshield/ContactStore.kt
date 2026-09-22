package dev.shahzad.prefixshield

import android.Manifest
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class DeviceContact(
    val contactId: Long,
    val name: String,
    val numbers: List<String>,
    val starred: Boolean,
    val givenName: String = "",
    val familyName: String = "",
    val company: String = "",
    val email: String = "",
    val note: String = ""
) {
    val primaryNumber: String get() = numbers.firstOrNull().orEmpty()
}

data class ContactDraft(
    val givenName: String,
    val familyName: String,
    val company: String,
    val phone: String,
    val email: String,
    val note: String,
    val account: ContactAccount
) {
    val displayName: String
        get() = listOf(givenName.trim(), familyName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { company.trim() }
            .ifBlank { phone.trim() }
}

data class ContactAccount(
    val name: String,
    val type: String,
    val label: String
) {
    val isSim: Boolean get() = type == TYPE_SIM
    val isGoogle: Boolean get() = type == "com.google"

    companion object {
        const val TYPE_PHONE = "phone"
        const val TYPE_SIM = "sim"
    }
}

object ContactStore {
    fun list(context: Context): List<DeviceContact> {
        if (!canRead(context)) return emptyList()
        val byId = LinkedHashMap<Long, MutableContact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.STARRED
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val starCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
            while (cursor.moveToNext()) {
                val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                val number = if (numberCol >= 0) cursor.getString(numberCol).orEmpty() else ""
                if (number.isBlank()) continue
                val row = byId.getOrPut(id) {
                    MutableContact(
                        id = id,
                        name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else "",
                        starred = starCol >= 0 && cursor.getInt(starCol) == 1
                    )
                }
                if (row.numbers.none { it == number }) row.numbers += number
            }
        }
        fillDetails(context, byId)
        return byId.values.map { row ->
            DeviceContact(
                contactId = row.id,
                name = row.display.ifBlank { row.numbers.first() },
                numbers = row.numbers.toList(),
                starred = row.starred,
                givenName = row.givenName,
                familyName = row.familyName,
                company = row.company,
                email = row.email,
                note = row.note
            )
        }
    }

    fun accounts(context: Context): List<ContactAccount> {
        val found = LinkedHashMap<String, ContactAccount>()
        found["phone"] = ContactAccount("", ContactAccount.TYPE_PHONE, "Phone")
        found["sim"] = ContactAccount("SIM", ContactAccount.TYPE_SIM, "SIM")
        if (canRead(context)) {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts.ACCOUNT_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                val typeCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                while (cursor.moveToNext()) {
                    val type = if (typeCol >= 0) cursor.getString(typeCol).orEmpty() else ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                    if (type == "com.google" && name.isNotBlank()) {
                        found["google:$name"] = ContactAccount(name, type, name)
                    }
                }
            }
        }
        runCatching {
            AccountManager.get(context).getAccountsByType("com.google").forEach { account ->
                found["google:${account.name}"] = ContactAccount(account.name, account.type, account.name)
            }
        }
        return found.values.toList()
    }

    fun insert(context: Context, draft: ContactDraft): Boolean {
        if (!canWrite(context)) return false
        val phone = draft.phone.trim()
        if (phone.isBlank()) return false
        val display = draft.displayName.ifBlank { phone }
        if (draft.account.isSim) return insertSim(context, display, phone)
        val ops = ArrayList<ContentProviderOperation>()
        val raw = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        if (draft.account.type == ContactAccount.TYPE_PHONE) {
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        } else {
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, draft.account.type)
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, draft.account.name)
        }
        ops += raw.build()
        ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, display)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, draft.givenName.trim())
            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, draft.familyName.trim())
            .build()
        ops += phoneInsert(phone)
        if (draft.company.isNotBlank()) ops += companyInsert(draft.company.trim())
        if (draft.email.isNotBlank()) ops += emailInsert(draft.email.trim())
        if (draft.note.isNotBlank()) ops += noteInsert(draft.note.trim())
        return runCatching {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        }.getOrDefault(false)
    }

    fun update(context: Context, contactId: Long, draft: ContactDraft): Boolean {
        if (!canWrite(context) || contactId <= 0L) return false
        val phone = draft.phone.trim()
        if (phone.isBlank()) return false
        val display = draft.displayName.ifBlank { phone }
        runCatching {
            upsertMime(context, contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE) {
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, display)
                put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, draft.givenName.trim())
                put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, draft.familyName.trim())
            }
        }
        runCatching {
            upsertMime(context, contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) {
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            }
        }
        writeOptional(
            context,
            contactId,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            draft.company.trim()
        ) {
            put(ContactsContract.CommonDataKinds.Organization.COMPANY, draft.company.trim())
            put(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
        }
        writeOptional(
            context,
            contactId,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            draft.email.trim()
        ) {
            put(ContactsContract.CommonDataKinds.Email.ADDRESS, draft.email.trim())
            put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
        }
        writeOptional(
            context,
            contactId,
            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
            draft.note.trim()
        ) {
            put(ContactsContract.CommonDataKinds.Note.NOTE, draft.note.trim())
        }
        return true
    }

    fun delete(context: Context, contactId: Long): Boolean {
        if (!canWrite(context) || contactId <= 0L) return false
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        return context.contentResolver.delete(uri, null, null) > 0
    }

    fun setStarred(context: Context, contactId: Long, starred: Boolean): Boolean {
        if (!canWrite(context)) return false
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        return context.contentResolver.update(
            uri,
            ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) },
            null,
            null
        ) > 0
    }

    fun suggestions(query: String, contacts: List<DeviceContact>, logs: List<PhoneLogEntry>): List<DeviceContact> {
        val digits = NumberMatcher.extractNumber(query)
        if (digits.length < 2) {
            return logs.mapNotNull { log ->
                val number = log.number
                if (number.isBlank()) return@mapNotNull null
                DeviceContact(0L, log.name?.takeIf { it.isNotBlank() } ?: number, listOf(number), false)
            }.distinctBy { NumberMatcher.extractNumber(it.primaryNumber) }.take(40)
        }
        fun matches(number: String): Boolean {
            val n = NumberMatcher.extractNumber(number)
            val local = if (n.length > 10) n.takeLast(10) else n
            return n.contains(digits) || local.startsWith(digits) || local.contains(digits)
        }
        val fromContacts = contacts.filter { contact ->
            contact.numbers.any { matches(it) } || t9(contact.name).contains(digits)
        }
        val extra = logs.mapNotNull { log ->
            val number = log.number
            if (number.isBlank() || !matches(number)) return@mapNotNull null
            if (fromContacts.any { it.numbers.any { n -> NumberMatcher.extractNumber(n) == NumberMatcher.extractNumber(number) } }) {
                return@mapNotNull null
            }
            DeviceContact(0L, log.name?.takeIf { it.isNotBlank() } ?: number, listOf(number), false)
        }
        return (fromContacts + extra).distinctBy { NumberMatcher.extractNumber(it.primaryNumber) }.take(6)
    }

    private fun insertSim(context: Context, name: String, number: String): Boolean {
        val values = ContentValues().apply {
            put("tag", name)
            put("number", number)
        }
        val uris = listOf("content://icc/adn", "content://sim/adn")
        return uris.any { raw ->
            runCatching { context.contentResolver.insert(Uri.parse(raw), values) != null }.getOrDefault(false)
        }
    }

    private fun rawContactId(context: Context, contactId: Long): Long? {
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    private fun t9(name: String): String = buildString(name.length) {
        for (ch in name.lowercase()) {
            append(
                when (ch) {
                    in 'a'..'c' -> '2'
                    in 'd'..'f' -> '3'
                    in 'g'..'i' -> '4'
                    in 'j'..'l' -> '5'
                    in 'm'..'o' -> '6'
                    in 'p'..'s' -> '7'
                    in 't'..'v' -> '8'
                    in 'w'..'z' -> '9'
                    else -> continue
                }
            )
        }
    }

    private fun canRead(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun canWrite(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun fillDetails(context: Context, byId: Map<Long, MutableContact>) {
        if (byId.isEmpty() || !canRead(context)) return
        val nameMime = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        val orgMime = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
        val emailMime = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
        val noteMime = ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3
            ),
            "${ContactsContract.Data.MIMETYPE} IN (?,?,?,?)",
            arrayOf(nameMime, orgMime, emailMime, noteMime),
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val mimeCol = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1 = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data2 = cursor.getColumnIndex(ContactsContract.Data.DATA2)
            val data3 = cursor.getColumnIndex(ContactsContract.Data.DATA3)
            while (cursor.moveToNext()) {
                val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                val row = byId[id] ?: continue
                val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else continue
                val first = if (data1 >= 0) cursor.getString(data1).orEmpty() else ""
                when (mime) {
                    nameMime -> {
                        val given = if (data2 >= 0) cursor.getString(data2).orEmpty() else ""
                        val family = if (data3 >= 0) cursor.getString(data3).orEmpty() else ""
                        if (given.isNotBlank()) row.givenName = given
                        if (family.isNotBlank()) row.familyName = family
                        if (row.name.isBlank() && first.isNotBlank()) row.name = first
                    }
                    orgMime -> if (first.isNotBlank()) row.company = first
                    emailMime -> if (first.isNotBlank() && row.email.isBlank()) row.email = first
                    noteMime -> if (first.isNotBlank()) row.note = first
                }
            }
        }
    }

    private fun phoneInsert(phone: String): ContentProviderOperation {
        return ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            .build()
    }

    private fun companyInsert(company: String): ContentProviderOperation {
        return ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
            .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
            .build()
    }

    private fun emailInsert(email: String): ContentProviderOperation {
        return ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
            .build()
    }

    private fun noteInsert(note: String): ContentProviderOperation {
        return ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note)
            .build()
    }

    private fun writeOptional(
        context: Context,
        contactId: Long,
        mime: String,
        value: String,
        fill: ContentValues.() -> Unit
    ) {
        if (value.isBlank()) {
            deleteMime(context, contactId, mime)
        } else {
            runCatching { upsertMime(context, contactId, mime, fill) }
        }
    }

    private fun upsertMime(
        context: Context,
        contactId: Long,
        mime: String,
        fill: ContentValues.() -> Unit
    ): Boolean {
        val values = ContentValues().apply(fill)
        val updated = context.contentResolver.update(
            ContactsContract.Data.CONTENT_URI,
            values,
            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(contactId.toString(), mime)
        )
        if (updated > 0) return true
        val rawId = rawContactId(context, contactId) ?: return false
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
        values.put(ContactsContract.Data.MIMETYPE, mime)
        return context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
    }

    private fun deleteMime(context: Context, contactId: Long, mime: String) {
        runCatching {
            context.contentResolver.delete(
                ContactsContract.Data.CONTENT_URI,
                "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(contactId.toString(), mime)
            )
        }
    }

    private data class MutableContact(
        val id: Long,
        var name: String,
        val starred: Boolean,
        val numbers: MutableList<String> = mutableListOf(),
        var givenName: String = "",
        var familyName: String = "",
        var company: String = "",
        var email: String = "",
        var note: String = ""
    ) {
        val display: String
            get() {
                val structured = listOf(givenName, familyName).filter { it.isNotBlank() }.joinToString(" ")
                return structured.ifBlank { name }.ifBlank { company }
            }
    }
}
