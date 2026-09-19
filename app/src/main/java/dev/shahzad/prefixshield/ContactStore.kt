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
    val starred: Boolean
) {
    val primaryNumber: String get() = numbers.firstOrNull().orEmpty()
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
        return byId.values.map {
            DeviceContact(it.id, it.name.ifBlank { it.numbers.first() }, it.numbers.toList(), it.starred)
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

    fun insert(context: Context, name: String, number: String, account: ContactAccount): Boolean {
        if (!canWrite(context)) return false
        val trimmedName = name.trim().ifBlank { number }
        val trimmedNumber = number.trim()
        if (trimmedNumber.isBlank()) return false
        if (account.isSim) return insertSim(context, trimmedName, trimmedNumber)
        val ops = ArrayList<ContentProviderOperation>()
        val raw = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        if (account.type == ContactAccount.TYPE_PHONE) {
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        } else {
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
            raw.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        }
        ops += raw.build()
        ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, trimmedName)
            .build()
        ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, trimmedNumber)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            .build()
        return runCatching {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        }.getOrDefault(false)
    }

    fun update(context: Context, contactId: Long, name: String, number: String): Boolean {
        if (!canWrite(context) || contactId <= 0L) return false
        val display = name.trim().ifBlank { number.trim() }
        val phone = number.trim()
        runCatching {
            context.contentResolver.update(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, display)
                },
                "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            )
        }
        val updated = runCatching {
            context.contentResolver.update(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                },
                "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            )
        }.getOrDefault(0)
        if (updated == 0 && phone.isNotBlank()) {
            val rawId = rawContactId(context, contactId) ?: return false
            context.contentResolver.insert(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                }
            )
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
        if (digits.length < 2) return emptyList()
        val fromContacts = contacts.filter { contact ->
            contact.numbers.any { NumberMatcher.extractNumber(it).contains(digits) } ||
                t9(contact.name).contains(digits)
        }
        val extra = logs.mapNotNull { log ->
            val number = log.number
            if (number.isBlank()) return@mapNotNull null
            if (!NumberMatcher.extractNumber(number).contains(digits)) return@mapNotNull null
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

    private data class MutableContact(
        val id: Long,
        val name: String,
        val starred: Boolean,
        val numbers: MutableList<String> = mutableListOf()
    )
}
