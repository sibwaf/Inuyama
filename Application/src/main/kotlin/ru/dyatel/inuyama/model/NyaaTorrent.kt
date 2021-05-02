package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.DateTimeConverter
import java.util.TimeZone

@Entity
data class NyaaTorrent(
    @Id(assignable = true) var id: Long = 0,
    var title: String = "",
    var hash: String = "",

    @Convert(converter = DateTimeConverter::class, dbType = String::class)
    var updateDatetime: DateTime = DateTime.now(TimeZone.getDefault()),

    var dispatched: Boolean = false
) {
    lateinit var watch: ToOne<NyaaWatch>
}
