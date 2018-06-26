package ru.dyatel.inuyama.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

@Entity
data class RutrackerWatch(
        @Id(assignable = true) var id: Long = 0,
        var hash: String? = null,
        var description: String = ""
) {
    lateinit var directory: ToOne<Directory?>
}
