package dev.shahzad.prefixshield

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat

data class PhoneLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val atMillis: Long,
    val durationSec: Long
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }

    val typeLabel: String
        get() = when (type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE -> "Missed"
            CallLog.Calls.REJECTED_TYPE -> "Rejected"
            CallLog.Calls.BLOCKED_TYPE -> "Blocked"
            CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
            else -> "Call"
        }

    val durationLabel: String?
        get() {
            if (durationSec <= 0L) return null
            val minutes = durationSec / 60
            val seconds = durationSec % 60
            return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        }
}

object PhoneLogStore {
    @SuppressLint("MissingPermission")
    fun list(context: Context, limit: Int = 100): List<PhoneLogEntry> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val entries = mutableListOf<PhoneLogEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
            val numberCol = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeCol = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateCol = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationCol = cursor.getColumnIndex(CallLog.Calls.DURATION)
            while (cursor.moveToNext() && entries.size < limit) {
                entries += PhoneLogEntry(
                    id = if (idCol >= 0) cursor.getLong(idCol) else entries.size.toLong(),
                    number = if (numberCol >= 0) cursor.getString(numberCol).orEmpty() else "",
                    name = if (nameCol >= 0) cursor.getString(nameCol) else null,
                    type = if (typeCol >= 0) cursor.getInt(typeCol) else 0,
                    atMillis = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                    durationSec = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                )
            }
        }
        return entries
    }

    fun delete(context: Context, id: Long) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        context.contentResolver.delete(
            CallLog.Calls.CONTENT_URI,
            "${CallLog.Calls._ID}=?",
            arrayOf(id.toString())
        )
    }

    fun clear(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        context.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
    }
}
