package ru.sibwaf.inuyama.common.utilities

import java.io.InputStream

class ChainedInputStream(private val streams: List<InputStream>) : InputStream() {

    constructor(vararg streams: InputStream) : this(streams.asList())

    private var streamIterator: Iterator<InputStream>? = null
    private var current: InputStream? = null

    private val singleReadBuffer = ByteArray(1)

    override fun read(): Int {
        if (read(singleReadBuffer) <= 0) {
            return -1
        }
        return singleReadBuffer[0].toInt()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val current = if (current == null) {
            val streamIterator = streamIterator ?: streams.iterator()
            this.streamIterator = streamIterator

            if (streamIterator.hasNext()) {
                streamIterator.next().also { this.current = it }
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

    override fun reset() {
        for (stream in streams) {
            stream.reset()
        }

        streamIterator = null
        current = null
    }

    override fun close() {
        for (stream in streams) {
            stream.close()
        }
    }
}
