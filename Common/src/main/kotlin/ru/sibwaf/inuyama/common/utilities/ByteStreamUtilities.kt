package ru.sibwaf.inuyama.common.utilities

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun ByteArrayOutputStream.writeInt(value: Int) {
    write((value shr 24) and 0xFF)
    write((value shr 16) and 0xFF)
    write((value shr 8) and 0xFF)
    write(value and 0xFF)
}

fun ByteArrayInputStream.readInt(): Int {
    val buffer = ByteArray(4)
    read(buffer)
    return ((buffer[0].toInt() and 0xFF) shl 24) or
            ((buffer[1].toInt() and 0xFF) shl 16) or
            ((buffer[2].toInt() and 0xFF) shl 8) or
            (buffer[3].toInt() and 0xFF)
}