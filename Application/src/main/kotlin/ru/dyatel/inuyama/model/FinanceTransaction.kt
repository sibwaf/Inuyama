package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime

sealed interface FinanceTransaction {
    val datetime: DateTime
}
