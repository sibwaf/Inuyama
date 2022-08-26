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
    val datetime: OffsetDateTime,
    val categoryId: String,
    val amount: Double
)
