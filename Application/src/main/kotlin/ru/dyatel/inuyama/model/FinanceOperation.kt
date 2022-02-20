package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.DateTimeConverter
import ru.dyatel.inuyama.utilities.UuidConverter
import java.util.TimeZone
import java.util.UUID

@Entity
data class FinanceOperation(
    @Id var id: Long = 0,
    var amount: Double = 0.0,

    @Deprecated("use FinanceReceipt's datetime")
    @Convert(converter = DateTimeConverter::class, dbType = String::class)
    var datetime: DateTime = DateTime.now(TimeZone.getDefault()),

    // objectbox refuses to insert multiple equal entities at the same time
    @Convert(converter = UuidConverter::class, dbType = String::class)
    var guid: UUID = UUID.randomUUID(),

    var description: String? = null
) {
    lateinit var categories: ToMany<FinanceCategory>
    lateinit var receipt: ToOne<FinanceReceipt>

    @Deprecated("use FinanceReceipt's account")
    lateinit var account: ToOne<FinanceAccount>

    @Deprecated("Use categories instead")
    lateinit var category: ToOne<FinanceCategory?>
}
