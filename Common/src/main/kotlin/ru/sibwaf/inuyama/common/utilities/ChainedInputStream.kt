package ru.sibwaf.inuyama.common.utilities

import java.io.InputStream

class ChainedInputStream(streams: Iterable<InputStream>) : InputStream() {

    constructor(vararg streams: InputStream) : this(streams.asList())

    private val streams = streams.toList().iterator()
    private var current: InputStream? = null

    private val singleReadBuffer = ByteArray(1)

    override fun read(): Int {
        if (read(singleReadBuffer, 0, singleReadBuffer.size) <= 0) {
            return -1
        }
        return singleReadBuffer[0].toInt()
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val current = if (current == null) {
            if (streams.hasNext()) {
                streams.next().also { this.current = it }
            } else {
                return -1
            }
        } else {
            current!!
        }

        val count = current.read(b, off, len)
        if (count < 0) {
            current.close()
            this.current = null
            return read(b, off, len)
        }
        return count
    }

    override fun close() {
        for (stream in sequenceOf(current) + streams.asSequence()) {
            stream?.close()
        }
    }
}
