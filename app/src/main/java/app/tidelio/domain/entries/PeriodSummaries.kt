package app.tidelio.domain.entries

data class PeriodSummary(
    val period: DayPeriod,
    val volumeMl: Long,
    val entryCount: Int,
    /** period volume / daily volume × 100; null when the day has no recorded volume. */
    val sharePercent: Double?,
)

object PeriodSummaries {
    /** Every entry is counted in exactly one period. */
    fun of(entries: List<WaterEntry>): List<PeriodSummary> {
        val dayTotal = entries.sumOf { it.amountMl.toLong() }
        val byPeriod = entries.groupBy { it.period }
        return DayPeriod.entries.map { period ->
            val list = byPeriod[period].orEmpty()
            val volume = list.sumOf { it.amountMl.toLong() }
            PeriodSummary(
                period = period,
                volumeMl = volume,
                entryCount = list.size,
                sharePercent = if (dayTotal > 0) volume * 100.0 / dayTotal else null,
            )
        }
    }
}
