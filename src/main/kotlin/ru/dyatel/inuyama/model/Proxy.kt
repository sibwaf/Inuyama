package ru.dyatel.inuyama.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Proxy(
        @Id var id: Long = 0,
        var host: String,
        var port: Int
)
