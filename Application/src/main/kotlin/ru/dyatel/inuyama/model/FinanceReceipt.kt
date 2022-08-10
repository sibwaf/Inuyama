package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime
import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.IndexType
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.DateTimeConverter
import java.util.TimeZone

@Entity
data class FinanceReceipt(
    @Id var id: Long = 0,

    @Convert(converter = DateTimeConverter::class, dbType = String::class)
    @Index(type = IndexType.VALUE)
    override var datetime: DateTime = DateTime.now(TimeZone.getDefault())
) : FinanceTransaction {
    lateinit var account: ToOne<FinanceAccount>

    @Backlink(to = "receipt")
    lateinit var operations: ToMany<FinanceOperation>
}
