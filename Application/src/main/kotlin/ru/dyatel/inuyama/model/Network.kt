package ru.dyatel.inuyama.model

import io.objectbox.Box
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.kotlin.query

@Entity
data class Network(
        @Id var id: Long = 0,
        var name: String,
        var bssid: String,
        var trusted: Boolean = false
)

fun Box<Network>.findByBssid(bssid: String): Network? {
    return query { equal(Network_.bssid, bssid) }.findUnique()
}