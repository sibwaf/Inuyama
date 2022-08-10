package ru.dyatel.inuyama.finance.dto

import hirondelle.date4j.DateTime

data class TransactionHistoryCursor(
    val lastReceiptId: Long?,
    val lastReceiptDatetime: DateTime?,
    val lastTransferId: Long?,
    val lastTransferDatetime: DateTime?,

    val finished: Boolean,
)
