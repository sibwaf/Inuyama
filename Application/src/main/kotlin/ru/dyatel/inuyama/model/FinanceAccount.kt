package ru.dyatel.inuyama.model

import io.objectbox.annotation.DefaultValue
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class FinanceAccount(
    @Id var id: Long = 0,
    var name: String = "",
    var initialBalance: Double = 0.0,
    var balance: Double = 0.0,
    @DefaultValue("") var currency: String = "",
    var quickAccess: Boolean = true,
    var disabled: Boolean = false,
)
