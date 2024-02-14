package ru.sibwaf.inuyama.finance.analytics

import kotlinx.coroutines.runBlocking
import ru.sibwaf.inuyama.finance.FinanceBackupDataProvider
import ru.sibwaf.inuyama.finance.FinanceOperationDto
import ru.sibwaf.inuyama.finance.rates.ExchangeRateProvider
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// todo: use db
class FinanceAnalyticService(
    private val dataProvider: FinanceBackupDataProvider,
    private val exchangeRateProvider: ExchangeRateProvider,
) {

    fun queryOperationSummary(
        deviceId: String,
        grouping: FinanceAnalyticGrouping?,
        filter: FinanceAnalyticFilter,
        targetCurrency: String,
    ): Map<String, Double> {
        return dataProvider.getOperations(deviceId)
            .filter { filter.direction == null || filter.direction == it.direction }
            .filter { filter.start <= it.datetime && filter.end > it.datetime }
            .groupBy(grouping.toKeyExtractor())
            .mapValues { (_, operations) ->
                operations.sumOf {
                    val exchangeRate = runBlocking {
                        exchangeRateProvider.getExchangeRate(
                            fromCurrency = it.account.currency,
                            toCurrency = targetCurrency,
                            date = it.datetime.toLocalDate(),
                        )
                    } ?: 0.0
                    it.amount * exchangeRate
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
        val timelineStep = ChronoUnit.MONTHS
        val timeline = generateTimeline(
            start = filter.start,
            end = filter.end,
            step = timelineStep,
            zoneOffset = zoneOffset
        )

        val data = dataProvider.getOperations(deviceId)
            .filter { filter.direction == null || filter.direction == it.direction }
            .filter { filter.start <= it.datetime && filter.end > it.datetime }
            .groupBy(grouping.toKeyExtractor())
            .mapValues { (_, operations) ->
                val operationsByTimelinePoint = operations.groupBy {
                    it.datetime.toTimelinePoint(timelineStep, zoneOffset)
                }
                timeline.map { point ->
                    operationsByTimelinePoint[point]?.sumOf {
                        val exchangeRate = runBlocking {
                            exchangeRateProvider.getExchangeRate(
                                fromCurrency = it.account.currency,
                                toCurrency = targetCurrency,
                                date = it.datetime.toLocalDate(),
                            )
                        } ?: 0.0
                        it.amount * exchangeRate
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
        val timelineStep = ChronoUnit.MONTHS
        val timeline = generateTimeline(
            start = start,
            end = end,
            step = timelineStep,
            zoneOffset = zoneOffset
        )

        data class BalanceChangeDto(val datetime: OffsetDateTime, val currency: String, val amount: Double)

        val allChanges = sequence {
            for (operation in dataProvider.getOperations(deviceId)) {
                yield(
                    BalanceChangeDto(
                        datetime = operation.datetime,
                        currency = operation.account.currency,
                        amount = operation.amount,
                    )
                )
            }
            for (transfer in dataProvider.getTransfers(deviceId)) {
                yield(
                    BalanceChangeDto(
                        datetime = transfer.datetime,
                        currency = transfer.fromAccount.currency,
                        amount = -transfer.amountFrom,
                    )
                )
                yield(
                    BalanceChangeDto(
                        datetime = transfer.datetime,
                        currency = transfer.toAccount.currency,
                        amount = transfer.amountTo,
                    )
                )
            }
        }

        val latestSavings = dataProvider.getAccounts(deviceId)
            .groupingBy { it.currency }
            .foldTo(mutableMapOf(), 0.0) { acc, it -> acc + it.balance }
        val changesByTimelinePoint = HashMap<OffsetDateTime, MutableMap<String, Double>>()

        for (change in allChanges) {
            if (change.datetime < start) {
                continue
            }

            if (change.datetime > end) {
                latestSavings.compute(change.currency) { _, it -> (it ?: 0.0) + change.amount }
            } else {
                val timelinePoint = change.datetime.toTimelinePoint(timelineStep, zoneOffset)
                val changes = changesByTimelinePoint.getOrPut(timelinePoint) { mutableMapOf() }
                changes.compute(change.currency) { _, it -> (it ?: 0.0) + change.amount }
            }
        }

        val data = timeline.asReversed()
            .runningFold(latestSavings as Map<String, Double>) { acc, timelinePoint ->
                val changes = changesByTimelinePoint[timelinePoint]
                if (changes != null) {
                    (changes.keys + acc.keys).associateWith { currency ->
                        acc.getOrDefault(currency, 0.0) - changes.getOrDefault(currency, 0.0)
                    }
                } else {
                    acc
                }
            }
            .asReversed()
            .drop(1)
            .zip(timeline)
            .map { (savingsByCurrency, timelinePoint) ->
                savingsByCurrency.asSequence().sumOf { (currency, amount) ->
                    val exchangeRate = runBlocking {
                        exchangeRateProvider.getExchangeRate(
                            fromCurrency = currency,
                            toCurrency = targetCurrency,
                            date = timelinePoint.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate(),
                        )
                    } ?: 0.0
                    amount * exchangeRate
                }
            }

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
            FinanceAnalyticGrouping.CATEGORY -> { it -> it.category.id }
            FinanceAnalyticGrouping.CURRENCY -> { it -> it.account.currency }
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
