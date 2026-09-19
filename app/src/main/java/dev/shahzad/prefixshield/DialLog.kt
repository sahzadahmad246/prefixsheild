package dev.shahzad.prefixshield

import android.provider.CallLog
import kotlin.math.abs
import kotlin.math.min

data class DialLogEntry(
    val key: String,
    val callLogId: Long?,
    val number: String,
    val name: String?,
    val type: Int,
    val atMillis: Long,
    val durationSec: Long,
    val blocked: Boolean,
    val matchedPrefix: String?
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }

    val typeLabel: String
        get() = when {
            blocked -> "Blocked"
            type == CallLog.Calls.INCOMING_TYPE -> "Incoming"
            type == CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            type == CallLog.Calls.MISSED_TYPE -> "Missed"
            type == CallLog.Calls.REJECTED_TYPE -> "Rejected"
            type == CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
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

enum class LogFilter { ALL, INCOMING, OUTGOING, MISSED, BLOCKED }

object DialLogMerger {
    fun merge(
        phoneLogs: List<PhoneLogEntry>,
        blockedCalls: List<BlockedCall>
    ): List<DialLogEntry> {
        val usedBlocked = mutableSetOf<Int>()
        val merged = ArrayList<DialLogEntry>(phoneLogs.size + blockedCalls.size)

        for (log in phoneLogs) {
            val digits = NumberMatcher.extractNumber(log.number)
            val matchIdx = blockedCalls.indices.firstOrNull { index ->
                index !in usedBlocked &&
                    sameCaller(digits, blockedCalls[index].number) &&
                    abs(log.atMillis - blockedCalls[index].atMillis) < 12_000
            }
            if (matchIdx != null) usedBlocked += matchIdx
            val blockedHit = matchIdx?.let { blockedCalls[it] }
            val blocked = blockedHit != null || log.type == CallLog.Calls.BLOCKED_TYPE
            merged += DialLogEntry(
                key = "c-${log.id}-${log.atMillis}",
                callLogId = log.id,
                number = log.number,
                name = log.name,
                type = log.type,
                atMillis = log.atMillis,
                durationSec = log.durationSec,
                blocked = blocked,
                matchedPrefix = blockedHit?.matchedPrefix
            )
        }

        blockedCalls.forEachIndexed { index, call ->
            if (index in usedBlocked) return@forEachIndexed
            merged += DialLogEntry(
                key = "b-${call.atMillis}-${call.number}",
                callLogId = null,
                number = call.number,
                name = null,
                type = CallLog.Calls.BLOCKED_TYPE,
                atMillis = call.atMillis,
                durationSec = 0,
                blocked = true,
                matchedPrefix = call.matchedPrefix
            )
        }

        return merged.sortedByDescending { it.atMillis }
    }

    fun matchesFilter(entry: DialLogEntry, filter: LogFilter): Boolean = when (filter) {
        LogFilter.ALL -> true
        LogFilter.INCOMING -> !entry.blocked && entry.type == CallLog.Calls.INCOMING_TYPE
        LogFilter.OUTGOING -> !entry.blocked && entry.type == CallLog.Calls.OUTGOING_TYPE
        LogFilter.MISSED -> !entry.blocked && (
            entry.type == CallLog.Calls.MISSED_TYPE || entry.type == CallLog.Calls.REJECTED_TYPE
            )
        LogFilter.BLOCKED -> entry.blocked
    }

    fun matchesQuery(entry: DialLogEntry, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        val digits = NumberMatcher.extractNumber(q)
        return entry.title.contains(q, ignoreCase = true) ||
            entry.number.contains(q, ignoreCase = true) ||
            (digits.isNotEmpty() && NumberMatcher.extractNumber(entry.number).contains(digits)) ||
            (entry.matchedPrefix?.contains(q, ignoreCase = true) == true)
    }

    private fun sameCaller(logDigits: String, blockedNumber: String): Boolean {
        val blockedDigits = NumberMatcher.extractNumber(blockedNumber)
        if (logDigits.isEmpty() || blockedDigits.isEmpty()) return false
        if (logDigits == blockedDigits) return true
        val keep = min(10, min(logDigits.length, blockedDigits.length))
        if (keep < 8) return logDigits.contains(blockedDigits) || blockedDigits.contains(logDigits)
        return logDigits.takeLast(keep) == blockedDigits.takeLast(keep)
    }
}
