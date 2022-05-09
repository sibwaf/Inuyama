package ru.dyatel.inuyama.pairing

import javax.crypto.SecretKey

data class PairedSession(val token: String, val key: SecretKey)
