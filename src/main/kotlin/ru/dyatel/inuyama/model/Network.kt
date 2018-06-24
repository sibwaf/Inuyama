package ru.dyatel.inuyama.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity data class Network(
        @Id var id: Long = 0,
        var name: String,
        var bssid: String,
        var trusted: Boolean = false
)