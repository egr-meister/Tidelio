package app.tidelio.ui.common

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** English interface: all user-facing formatting uses Locale.US for consistency. */
object Fmt {
    private val locale: Locale = Locale.US
    private val time24 = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dateLong = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)
    private val dateMedium = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
    private val dateMediumYear = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
    private val dateShort = DateTimeFormatter.ofPattern("MMM d", locale)
    private val month = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val weekdayNarrow = DateTimeFormatter.ofPattern("EEEEE", locale)
    private val weekdayShort = DateTimeFormatter.ofPattern("EEE", locale)

    fun number(value: Long): String = String.format(locale, "%,d", value)
    fun number(value: Int): String = number(value.toLong())
    fun ml(value: Long): String = "${number(value)} mL"
    fun ml(value: Int): String = ml(value.toLong())
    fun percent(value: Double): String = String.format(locale, "%.0f%%", value)

    fun time(value: LocalTime): String = time24.format(value)
    fun dateLong(value: LocalDate): String = dateLong.format(value)
    fun dateMedium(value: LocalDate): String = dateMedium.format(value)
    fun dateMediumYear(value: LocalDate): String = dateMediumYear.format(value)
    fun dateShort(value: LocalDate): String = dateShort.format(value)
    fun month(value: YearMonth): String = month.format(value)
    fun weekdayNarrow(value: LocalDate): String = weekdayNarrow.format(value)
    fun weekdayShort(value: LocalDate): String = weekdayShort.format(value)

    fun entries(count: Int): String = if (count == 1) "1 entry" else "$count entries"
}
