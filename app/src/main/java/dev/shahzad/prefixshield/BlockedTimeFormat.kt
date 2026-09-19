package dev.shahzad.prefixshield

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class DayListEntry<out T> {
    data class Header(val label: String) : DayListEntry<Nothing>()
    data class Item<T>(val value: T) : DayListEntry<T>()
}

object BlockedTimeFormat {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun formatBlockedAt(
        atMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val elapsedMs = (nowMillis - atMillis).coerceAtLeast(0)
        val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
        val callInstant = Instant.ofEpochMilli(atMillis).atZone(zoneId)
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val callDate = callInstant.toLocalDate()

        if (elapsedMinutes < 60 && callDate == nowDate) {
            return when {
                elapsedMs < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                elapsedMinutes == 1L -> "1 min ago"
                else -> "$elapsedMinutes min ago"
            }
        }

        val time = timeFormatter.format(callInstant)
        return if (callDate == nowDate) {
            time
        } else {
            "$time, ${dateFormatter.format(callInstant)}"
        }
    }

    fun sectionLabel(day: LocalDate, today: LocalDate): String {
        return when (day) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> dateFormatter.format(day)
        }
    }

    fun buildListEntries(
        calls: List<BlockedCall>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DayListEntry<BlockedCall>> {
        return groupByDay(calls, { it.atMillis }, nowMillis, zoneId)
    }

    fun <T> groupByDay(
        items: List<T>,
        timeOf: (T) -> Long,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DayListEntry<T>> {
        if (items.isEmpty()) return emptyList()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val entries = mutableListOf<DayListEntry<T>>()
        var lastDay: LocalDate? = null
        for (item in items.sortedByDescending(timeOf)) {
            val day = Instant.ofEpochMilli(timeOf(item)).atZone(zoneId).toLocalDate()
            if (day != lastDay) {
                entries += DayListEntry.Header(sectionLabel(day, today))
                lastDay = day
            }
            entries += DayListEntry.Item(item)
        }
        return entries
    }
}
