package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SouthAfricaFormatters {
    private val zaLocale = Locale("en", "ZA")

    fun formatZar(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(zaLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val absFormatted = nf.format(kotlin.math.abs(amount))
        return if (amount < 0) "-R $absFormatted" else "R $absFormatted"
    }

    fun formatZarCompact(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(zaLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        return if (amount < 0) "-R ${nf.format(kotlin.math.abs(amount))}" else "R ${nf.format(amount)}"
    }

    fun todayIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun currentMonthPrefix(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    fun currentDateTimeDisplay(): String {
        return SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", zaLocale).format(Date())
    }

    fun formatGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)
            if (parsed != null) SimpleDateFormat("d MMM yyyy", zaLocale).format(parsed) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatDisplayDateTime(isoDateTime: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(isoDateTime)
            if (parsed != null) SimpleDateFormat("d MMM, HH:mm", zaLocale).format(parsed) else isoDateTime
        } catch (_: Exception) {
            isoDateTime
        }
    }

    fun isWithinLast7Days(isoDate: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(isoDate) ?: return false
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            !date.before(cal.time)
        } catch (_: Exception) {
            false
        }
    }
}
