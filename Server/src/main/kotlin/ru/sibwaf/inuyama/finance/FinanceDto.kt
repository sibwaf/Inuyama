package ru.sibwaf.inuyama.finance

import java.time.OffsetDateTime

data class FinanceCategoryDto(
    val id: String,
    val name: String
)

data class FinanceAccountDto(
    val id: String,
    val name: String,
    val balance: Double,
    val currency: String,
)

data class FinanceOperationDto(
    val accountId: String,
    val datetime: OffsetDateTime,
    val categoryId: String,
    val amount: Double
)

data class FinanceTransferDto(
    val fromAccountId: String,
    val toAccountId: String,
    val amountFrom: Double,
    val amountTo: Double,
    val datetime: OffsetDateTime,
)
