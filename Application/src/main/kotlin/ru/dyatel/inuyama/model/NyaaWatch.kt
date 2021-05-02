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
data class NyaaWatch(
    @Id var id: Long = 0,
    var description: String = "",
    var query: String = "",

    @Convert(converter = DateTimeConverter::class, dbType = String::class)
    var startDatetime: DateTime = DateTime.today(TimeZone.getDefault()),

    var collectPath: String = "",
    var lastUpdate: Long? = null
) {
    lateinit var directory: ToOne<Directory?>
    @Backlink
    lateinit var torrents: ToMany<NyaaTorrent>
}
