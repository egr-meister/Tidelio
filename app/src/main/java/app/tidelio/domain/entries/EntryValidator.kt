package app.tidelio.domain.entries

import app.tidelio.domain.time.DateTimeResolver
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** Input limits for a single entry. These are form limits, not drinking guidance. */
object EntryLimits {
    const val MIN_ML = 1
    const val MAX_ML = 5_000
}

sealed interface AmountParse {
    data class Valid(val ml: Int) : AmountParse
    data class Invalid(val message: String) : AmountParse
}

/**
 * Parses a whole-millilitre amount within [min]..[max].
 * Accepts digits with optional grouping commas/spaces ("1,500"); rejects decimals and signs.
 */
fun parseWholeMl(text: String, min: Int, max: Int): AmountParse {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return AmountParse.Invalid("Enter an amount in mL.")
    if (trimmed.contains('.')) return AmountParse.Invalid("Use whole milliliters only.")
    val digits = trimmed.replace(",", "").replace(" ", "").replace("\u00A0", "")
    if (digits.isEmpty() || !digits.all { it in '0'..'9' }) {
        return AmountParse.Invalid("Use whole milliliters only.")
    }
    if (digits.length > 7) return AmountParse.Invalid(String.format(Locale.US, "Enter an amount from %,d to %,d mL.", min, max))
    val value = digits.toInt()
    if (value < min || value > max) {
        return AmountParse.Invalid(String.format(Locale.US, "Enter an amount from %,d to %,d mL.", min, max))
    }
    return AmountParse.Valid(value)
}

data class EntryInputErrors(
    val amount: String? = null,
    val date: String? = null,
    val time: String? = null,
) {
    val isEmpty: Boolean get() = amount == null && date == null && time == null
}

sealed interface EntryValidation {
    data class Valid(val amountMl: Int, val dateTime: ZonedDateTime) : EntryValidation
    data class Invalid(val errors: EntryInputErrors) : EntryValidation
}

object EntryValidator {

    fun validate(
        amountText: String,
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId,
        now: Instant,
    ): EntryValidation {
        var errors = EntryInputErrors()
        val amount = when (val parsed = parseWholeMl(amountText, EntryLimits.MIN_ML, EntryLimits.MAX_ML)) {
            is AmountParse.Valid -> parsed.ml
            is AmountParse.Invalid -> {
                errors = errors.copy(amount = parsed.message)
                null
            }
        }
        val today = now.atZone(zone).toLocalDate()
        var resolved: ZonedDateTime? = null
        if (date.isAfter(today)) {
            errors = errors.copy(date = "Choose today or a past date.")
        } else {
            when (val r = DateTimeResolver.resolve(date, time, zone)) {
                is DateTimeResolver.Resolution.Nonexistent -> errors = errors.copy(time = r.message)
                is DateTimeResolver.Resolution.Valid -> {
                    if (r.dateTime.toInstant().isAfter(now)) {
                        errors = errors.copy(time = "Future times are not allowed.")
                    } else {
                        resolved = r.dateTime
                    }
                }
            }
        }
        return if (errors.isEmpty && amount != null && resolved != null) {
            EntryValidation.Valid(amount, resolved)
        } else {
            EntryValidation.Invalid(errors)
        }
    }
}
