package ru.sibwaf.inuyama.finance.analytics

import java.time.OffsetDateTime

class FinanceAnalyticFilter(
    val start: OffsetDateTime,
    val end: OffsetDateTime,

    val direction: FinanceOperationDirection?
)

enum class FinanceOperationDirection {
    INCOME, EXPENSE
}
