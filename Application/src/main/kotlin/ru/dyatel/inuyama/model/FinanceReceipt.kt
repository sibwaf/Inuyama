package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime
import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.DateTimeConverter
import java.util.TimeZone

@Entity
data class FinanceReceipt(
    @Id var id: Long = 0,

    @Convert(converter = DateTimeConverter::class, dbType = String::class)
    var datetime: DateTime = DateTime.now(TimeZone.getDefault())
) {
    lateinit var account: ToOne<FinanceAccount>

    @Backlink(to = "receipt")
    lateinit var operations: ToMany<FinanceOperation>
}
