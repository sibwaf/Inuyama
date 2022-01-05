package ru.dyatel.inuyama.finance.dto

import ru.dyatel.inuyama.model.FinanceAccount

data class FinanceReceiptInfo(
    val account: FinanceAccount,
    val operations: List<FinanceOperationInfo>
)
