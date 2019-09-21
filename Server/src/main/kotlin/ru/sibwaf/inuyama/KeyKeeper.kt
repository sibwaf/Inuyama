package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import java.nio.file.Files
import java.nio.file.Paths

class KeyKeeper {

    val keyPair by lazy {
        val file = Paths.get("server.key")

        if (Files.exists(file)) {
            val bytes = Files.readAllBytes(file)
            return@lazy Encoding.decodeRSAKeyPair(bytes)
        } else {
            val pair = Cryptography.createRSAKeyPair()
            Files.write(file, Encoding.encodeRSAKeyPair(pair))
            return@lazy pair
        }
    }
}
