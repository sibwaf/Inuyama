package ru.sibwaf.inuyama.finance.analytics

import kotlinx.coroutines.runBlocking
import ru.sibwaf.inuyama.finance.CurrencyConverter
import ru.sibwaf.inuyama.finance.FinanceBackupDataProvider
import ru.sibwaf.inuyama.finance.FinanceOperationDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// todo: use db
class FinanceAnalyticService(
    private val dataProvider: FinanceBackupDataProvider,
    private val currencyConverter: CurrencyConverter,
) {

    fun queryOperationSummary(
        deviceId: String,
        grouping: FinanceAnalyticGrouping?,
        filter: FinanceAnalyticFilter,
        targetCurrency: String,
    ): Map<String, Double> {
        val accounts = dataProvider.getAccounts(deviceId).associateBy { it.id }

        runBlocking {
            currencyConverter.prepareCache(
                currencies = accounts.values.mapTo(HashSet()) { it.currency },
                start = filter.start,
                end = filter.end,
            )
        }

        return dataProvider.getOperations(deviceId)
            .filter { filter.direction == null || filter.direction == it.direction }
            .filter { filter.start <= it.datetime && filter.end > it.datetime }
            .groupBy(grouping.toKeyExtractor())
            .mapValues { (_, operations) ->
                operations.sumOf {
                    currencyConverter.convert(
                        amount = it.amount,
                        fromCurrency = accounts.getValue(it.accountId).currency,
                        toCurrency = targetCurrency,
                        datetime = it.datetime,
                    )
                }
            }
    }

    // todo: customizable timeline step
    fun queryOperationSeries(
        deviceId: String,
        grouping: FinanceAnalyticGrouping?,
        filter: FinanceAnalyticFilter,
        targetCurrency: String,
        zoneOffset: ZoneOffset,
    ): FinanceAnalyticSeriesDto {
        val accounts = dataProvider.getAccounts(deviceId).associateBy { it.id }

        val timelineStep = ChronoUnit.MONTHS
        val timeline = generateTimeline(
            start = filter.start,
            end = filter.end,
            step = timelineStep,
            zoneOffset = zoneOffset
        )

        runBlocking {
            currencyConverter.prepareCache(
                currencies = accounts.values.mapTo(HashSet()) { it.currency },
                start = filter.start,
                end = filter.end,
            )
        }

        val data = dataProvider.getOperations(deviceId)
            .filter { filter.direction == null || filter.direction == it.direction }
            .filter { filter.start <= it.datetime && filter.end > it.datetime }
            .groupBy(grouping.toKeyExtractor())
            .mapValues { (_, operations) ->
                val operationsByTimelinePoint = operations.groupBy { it.datetime.toTimelinePoint(timelineStep, zoneOffset) }
                timeline.map { point ->
                    operationsByTimelinePoint[point]?.sumOf {
                        currencyConverter.convert(
                            amount = it.amount,
                            fromCurrency = accounts.getValue(it.accountId).currency,
                            toCurrency = targetCurrency,
                            datetime = it.datetime,
                        )
                    } ?: 0.0
                }
            }

        return FinanceAnalyticSeriesDto(timeline, data)
    }

    // todo: customizable timeline step
    fun querySavingsSeries(
        deviceId: String,
        targetCurrency: String,
        start: OffsetDateTime,
        end: OffsetDateTime,
        zoneOffset: ZoneOffset,
    ): FinanceAnalyticSeriesDto {
        val accounts = dataProvider.getAccounts(deviceId).associateBy { it.id }

        val timelineStep = ChronoUnit.MONTHS
        val timeline = generateTimeline(
            start = start,
            end = end,
            step = timelineStep,
            zoneOffset = zoneOffset
        )

        runBlocking {
            currencyConverter.prepareCache(
                currencies = accounts.values.mapTo(HashSet()) { it.currency },
                start = start,
                end = end,
            )
        }

        val changeFromOperationsByTimelinePoint = dataProvider.getOperations(deviceId)
            .filter { it.datetime >= start }
            .groupBy { it.datetime.toTimelinePoint(timelineStep, zoneOffset) }
            .mapValues { (_, operations) ->
                operations.sumOf {
                    currencyConverter.convert(
                        amount = it.amount,
                        fromCurrency = accounts.getValue(it.accountId).currency,
                        toCurrency = targetCurrency,
                        datetime = it.datetime,
                    )
                }
            }

        val changeFromTransfersByTimelinePoint = dataProvider.getTransfers(deviceId)
            .filter { it.datetime >= start }
            .groupBy { it.datetime.toTimelinePoint(timelineStep, zoneOffset) }
            .mapValues { (_, transfers) ->
                transfers.sumOf {
                    val amountFrom = currencyConverter.convert(
                        amount = it.amountFrom,
                        fromCurrency = accounts.getValue(it.fromAccountId).currency,
                        toCurrency = targetCurrency,
                        datetime = it.datetime,
                    )
                    val amountTo = currencyConverter.convert(
                        amount = it.amountTo,
                        fromCurrency = accounts.getValue(it.toAccountId).currency,
                        toCurrency = targetCurrency,
                        datetime = it.datetime,
                    )
                    amountTo - amountFrom
                }
            }

        val changeAfterEnd = (changeFromOperationsByTimelinePoint.asSequence() + changeFromTransfersByTimelinePoint.asSequence())
            .filter { (timelinePoint, _) -> timelinePoint > timeline.last() }
            .sumOf { (_, amount) -> amount }

        val latestSavings = accounts.values.sumOf {
            currencyConverter.convert(
                amount = it.balance,
                fromCurrency = it.currency,
                toCurrency = targetCurrency,
                datetime = OffsetDateTime.now(),
            )
        }

        val data = timeline.asReversed()
            .runningFold(latestSavings - changeAfterEnd) { acc, timelinePoint ->
                var change = 0.0
                change += changeFromOperationsByTimelinePoint[timelinePoint] ?: 0.0
                change += changeFromTransfersByTimelinePoint[timelinePoint] ?: 0.0
                acc - change
            }
            .asReversed()
            .drop(1)

        return FinanceAnalyticSeriesDto(timeline, mapOf("all" to data))
    }

    private fun generateTimeline(
        start: OffsetDateTime,
        end: OffsetDateTime,
        step: ChronoUnit,
        zoneOffset: ZoneOffset,
    ): List<OffsetDateTime> {
        return generateSequence(start.toTimelinePoint(step, zoneOffset)) { it.plus(1, step) }
            .takeWhile { it < end }
            .toList()
    }

    private fun OffsetDateTime.toTimelinePoint(step: ChronoUnit, zoneOffset: ZoneOffset): OffsetDateTime {
        require(step == ChronoUnit.MONTHS) { "only month timeline steps are supported for now" }

        return withOffsetSameInstant(zoneOffset)
            .truncatedTo(ChronoUnit.DAYS)
            .with(TemporalAdjusters.firstDayOfMonth())
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
