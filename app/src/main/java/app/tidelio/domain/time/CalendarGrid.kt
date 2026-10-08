package app.tidelio.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Builds the cells of a month view: leading blanks (null) up to [firstDayOfWeek],
 * every day of the month, and trailing blanks so the size is a multiple of 7.
 */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells = ArrayList<LocalDate?>(42)
    repeat(leading) { cells.add(null) }
    for (day in 1..month.lengthOfMonth()) cells.add(month.atDay(day))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

/** Inclusive epoch-day range covering the month. */
fun YearMonth.epochDayRange(): LongRange = atDay(1).toEpochDay()..atEndOfMonth().toEpochDay()
