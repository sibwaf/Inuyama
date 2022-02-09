package ru.sibwaf.inuyama.finance

import java.time.OffsetDateTime

data class FinanceCategoryDto(
    val id: String,
    val name: String
)

data class FinanceAccountDto(
    val id: String,
    val name: String,
    val balance: Double
)

data class FinanceOperationDto(
    val datetime: OffsetDateTime,
    val categoryId: String,
    val amount: Double
)
