package ru.dyatel.inuyama.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Directory(
        @Id var id: Long = 0,
        var path: String
)
