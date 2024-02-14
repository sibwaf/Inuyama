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
    val account: FinanceAccountDto,
    val datetime: OffsetDateTime,
    val category: FinanceCategoryDto,
    val amount: Double
)

data class FinanceTransferDto(
    val fromAccount: FinanceAccountDto,
    val toAccount: FinanceAccountDto,
    val amountFrom: Double,
    val amountTo: Double,
    val datetime: OffsetDateTime,
)
