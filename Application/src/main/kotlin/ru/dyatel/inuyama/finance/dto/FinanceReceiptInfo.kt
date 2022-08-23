package ru.dyatel.inuyama.finance.dto

import hirondelle.date4j.DateTime
import ru.dyatel.inuyama.model.FinanceAccount

data class FinanceReceiptInfo(
    val direction: FinanceOperationDirection,
    val account: FinanceAccount,
    val operations: List<FinanceOperationInfo>,
    val datetime: DateTime,
)
