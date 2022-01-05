package ru.dyatel.inuyama.finance.dto

import ru.dyatel.inuyama.model.FinanceCategory

data class FinanceOperationInfo(
    val category: FinanceCategory,
    val amount: Double,
    val description: String?
)
