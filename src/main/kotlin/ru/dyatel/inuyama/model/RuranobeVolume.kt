package ru.dyatel.inuyama.model

import hirondelle.date4j.DateTime
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.DateTimeConverter

data class RuranobeVolume(
        @Id(assignable = true) var id: Long = 0,
        var order: Int = 0,

        var url: String = "",
        var coverUrl: String? = null,
        var title: String = "",
        var status: String = "",

        @Convert(converter = DateTimeConverter::class, dbType = String::class)
        var updateDatetime: DateTime? = null,

        var dispatched: Boolean = false
) {
    lateinit var project: ToOne<RuranobeProject>
}
