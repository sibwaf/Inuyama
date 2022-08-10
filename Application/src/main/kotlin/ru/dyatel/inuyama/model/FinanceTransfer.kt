package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.IndexType
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.DateTimeConverter
import java.util.TimeZone

@Entity
data class FinanceTransfer(
    @Id var id: Long = 0,
    var amount: Double = 0.0,

    @Convert(converter = DateTimeConverter::class, dbType = String::class)
    @Index(type = IndexType.VALUE)
    override var datetime: DateTime = DateTime.now(TimeZone.getDefault())
) : FinanceTransaction {
    lateinit var from: ToOne<FinanceAccount>
    lateinit var to: ToOne<FinanceAccount>
}
