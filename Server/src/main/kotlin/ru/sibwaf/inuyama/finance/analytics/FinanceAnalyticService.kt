package ru.sibwaf.inuyama.finance.analytics

import ru.sibwaf.inuyama.finance.FinanceBackupDataProvider
import ru.sibwaf.inuyama.finance.FinanceOperationDto

// todo: use db
class FinanceAnalyticService(private val dataProvider: FinanceBackupDataProvider) {

    fun querySummary(deviceId: String, grouping: FinanceAnalyticGrouping?, filter: FinanceAnalyticFilter): Map<String, Double> {
        val keyExtractor: (FinanceOperationDto) -> String = when (grouping) {
            FinanceAnalyticGrouping.DIRECTION -> { it -> it.direction.name }
            FinanceAnalyticGrouping.CATEGORY -> { it -> it.categoryId }
            null -> { _ -> "all" }
        }

        return dataProvider.getOperations(deviceId)
            .filter { filter.direction == null || filter.direction == it.direction }
            .filter { filter.start <= it.datetime && filter.end > it.datetime }
            .groupBy(keyExtractor)
            .mapValues { (_, operations) -> operations.sumByDouble { it.amount } }
    }

    private val FinanceOperationDto.direction: FinanceOperationDirection
        get() = when {
            amount >= 0 -> FinanceOperationDirection.INCOME
            amount < 0 -> FinanceOperationDirection.EXPENSE
            else -> throw RuntimeException("amount is neither positive nor negative")
        }
}
