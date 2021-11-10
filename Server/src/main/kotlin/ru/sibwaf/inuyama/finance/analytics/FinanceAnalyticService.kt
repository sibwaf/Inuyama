package ru.sibwaf.inuyama.finance.analytics

import ru.sibwaf.inuyama.finance.FinanceBackupDataProvider
import ru.sibwaf.inuyama.finance.FinanceOperationDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// todo: use db
class FinanceAnalyticService(private val dataProvider: FinanceBackupDataProvider) {

    fun querySummary(deviceId: String, grouping: FinanceAnalyticGrouping?, filter: FinanceAnalyticFilter): Map<String, Double> {
        return dataProvider.getOperations(deviceId)
            .filter { filter.direction == null || filter.direction == it.direction }
            .filter { filter.start <= it.datetime && filter.end > it.datetime }
            .groupBy(grouping.toKeyExtractor())
            .mapValues { (_, operations) -> operations.sumByDouble { it.amount } }
    }

    // todo: customizable timeline step
    fun querySeries(
        deviceId: String,
        grouping: FinanceAnalyticGrouping?,
        filter: FinanceAnalyticFilter,
        zoneOffset: ZoneOffset
    ): FinanceAnalyticSeriesDto {
        val timelineStep = ChronoUnit.MONTHS

        fun OffsetDateTime.toTimelinePoint(): OffsetDateTime {
            return withOffsetSameInstant(zoneOffset)
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.firstDayOfMonth())
        }

        val timeline = generateSequence(filter.start.toTimelinePoint()) { it.plus(1, timelineStep) }
            .takeWhile { it < filter.end }
            .toList()

        val data = dataProvider.getOperations(deviceId)
            .filter { filter.direction == null || filter.direction == it.direction }
            .filter { filter.start <= it.datetime && filter.end > it.datetime }
            .groupBy(grouping.toKeyExtractor())
            .mapValues { (_, operations) ->
                val operationsByTimeUnit = operations.groupBy { it.datetime.toTimelinePoint() }
                timeline.map { point -> operationsByTimeUnit[point]?.sumByDouble { it.amount } ?: 0.0 }
            }

        return FinanceAnalyticSeriesDto(timeline, data)
    }

    private fun FinanceAnalyticGrouping?.toKeyExtractor(): (FinanceOperationDto) -> String {
        return when (this) {
            FinanceAnalyticGrouping.DIRECTION -> { it -> it.direction.name }
            FinanceAnalyticGrouping.CATEGORY -> { it -> it.categoryId }
            null -> { _ -> "all" }
        }
    }

    private val FinanceOperationDto.direction: FinanceOperationDirection
        get() = when {
            amount >= 0 -> FinanceOperationDirection.INCOME
            amount < 0 -> FinanceOperationDirection.EXPENSE
            else -> throw RuntimeException("amount is neither positive nor negative")
        }
}
