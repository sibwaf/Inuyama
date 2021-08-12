package ru.dyatel.inuyama.utilities

import ru.dyatel.inuyama.BuildConfig

inline fun debugOnly(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}

inline fun releaseOnly(block: () -> Unit) {
    if (!BuildConfig.DEBUG) {
        block()
    }
}
