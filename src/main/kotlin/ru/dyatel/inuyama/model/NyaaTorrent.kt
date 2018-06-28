package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.DateTimeConverter
import ru.dyatel.inuyama.currentDatetime

@Entity
data class NyaaTorrent(
        @Id(assignable = true) var id: Long = 0,
        var title: String = "",
        var link: String = "",
        var hash: String = "",

        @Convert(converter = DateTimeConverter::class, dbType = String::class)
        var updateDatetime: DateTime = currentDatetime,

        var dispatched: Boolean = false
) {
    lateinit var watch: ToOne<NyaaWatch>
}
