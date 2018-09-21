package ru.dyatel.inuyama.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

@Entity
data class ProxyBinding(
        @Id(assignable = true) var id: Long = 0
) {
    lateinit var proxy: ToOne<Proxy?>
}
