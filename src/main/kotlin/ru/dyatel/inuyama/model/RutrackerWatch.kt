package ru.dyatel.inuyama.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

@Entity
data class RutrackerWatch(
        @Id(assignable = true) var id: Long = 0,
        var description: String = "",
        var magnet: String? = null,
        var lastUpdate: Long? = null,
        var updateDispatched: Boolean = false
) {
    lateinit var directory: ToOne<Directory?>
}
