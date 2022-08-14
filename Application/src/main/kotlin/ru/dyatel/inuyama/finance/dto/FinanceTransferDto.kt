package ru.dyatel.inuyama.finance.dto

import hirondelle.date4j.DateTime
import ru.dyatel.inuyama.model.FinanceAccount

data class FinanceTransferDto(
    val fromAccount: FinanceAccount,
    val toAccount: FinanceAccount,
    val amountFrom: Double,
    val amountTo: Double,
    val datetime: DateTime,
)
