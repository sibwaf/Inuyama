package ru.sibwaf.inuyama.common.utilities

object MagnetParser {

    private fun getQuery(magnet: String): Map<String, String> {
        val magnetParts = magnet.split("?")
        if (magnetParts.size != 2) {
            throw MagnetParsingException("Invalid magnet: $magnet")
        }

        return magnetParts[1].split("&")
                .map {
                    val parts = it.split("=")
                    if (parts.size != 2) {
                        throw MagnetParsingException("Invalid parameter: $it")
                    }

                    Pair(parts[0], parts[1])
                }
                .toMap()
    }

    fun extractHash(magnet: String) =
            getQuery(magnet)
                    .getOrElse("xt") { throw MagnetParsingException("xt not found") }
                    .split(":")
                    .last()

}

class MagnetParsingException : RuntimeException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

}
