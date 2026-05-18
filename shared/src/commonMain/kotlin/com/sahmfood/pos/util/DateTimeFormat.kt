package com.sahmfood.pos.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MIN_MS = 946_684_800_000L   // 2000-01-01 UTC
private const val MAX_MS = 4_102_444_800_000L // 2100-01-01 UTC

/** Formats an [Instant] for UI/receipts without invalid NSDate/timezone crashes on iOS. */
fun Instant.toDisplayDateTime(): String {
    val instant = Instant.fromEpochMilliseconds(normalizeEpochMillis(toEpochMilliseconds()))
    for (zone in displayTimeZones()) {
        val formatted = runCatching { instant.formatIn(zone) }.getOrNull()
        if (formatted != null) return formatted
    }
    return "—"
}

private fun displayTimeZones(): List<TimeZone> {
    val system = runCatching { TimeZone.currentSystemDefault() }.getOrNull()
    return listOfNotNull(system, TimeZone.UTC).distinct()
}

private fun Instant.formatIn(zone: TimeZone): String? {
    val dt = toLocalDateTime(zone)
    if (dt.year !in 2000..2100) return null
    return dt.toDisplayString()
}

private fun LocalDateTime.toDisplayString(): String {
    val mo = monthNumber.toString().padStart(2, '0')
    val day = dayOfMonth.toString().padStart(2, '0')
    val h = hour.toString().padStart(2, '0')
    val mi = minute.toString().padStart(2, '0')
    return "$year-$mo-$day $h:$mi"
}

/** Corrects epoch seconds stored as milliseconds, or out-of-range values. */
private fun normalizeEpochMillis(raw: Long): Long {
    if (raw in MIN_MS..MAX_MS) return raw
    if (raw in 1_000_000_000L..9_999_999_999L) return raw * 1_000
    return raw.coerceIn(MIN_MS, MAX_MS)
}
