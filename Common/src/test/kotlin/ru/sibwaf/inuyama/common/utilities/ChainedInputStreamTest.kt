package ru.sibwaf.inuyama.common.utilities

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contentEquals
import strikt.assertions.isEmpty
import strikt.assertions.isTrue
import java.io.InputStream

class ChainedInputStreamTest {

    @Test
    fun `Test reading byte-by-byte`() {
        val first = ByteArray(4) { it.toByte() }
        val second = ByteArray(4) { (it + 4).toByte() }

        val chainedStream = ChainedInputStream(
            first.inputStream(),
            second.inputStream(),
        )

        val result = ArrayList<Byte>()
        while (true) {
            val byte = chainedStream.read()
            if (byte >= 0) {
                result.add(byte.toByte())
            } else {
                break
            }
        }

        expectThat(result.toByteArray())
            .contentEquals(first + second)
    }

    @Test
    fun `Test reading with buffer size equal to part size`() {
        val first = ByteArray(4) { it.toByte() }
        val second = ByteArray(4) { (it + 4).toByte() }

        val chainedStream = ChainedInputStream(
            first.inputStream(),
            second.inputStream(),
        )

        expectThat(readStream(chainedStream, 4))
            .contentEquals(first + second)
    }

    @Test
    fun `Test reading with buffer size less than part size`() {
        val first = ByteArray(4) { it.toByte() }
        val second = ByteArray(4) { (it + 4).toByte() }

        val chainedStream = ChainedInputStream(
            first.inputStream(),
            second.inputStream(),
        )

        expectThat(readStream(chainedStream, 3))
            .contentEquals(first + second)
    }

    @Test
    fun `Test reading with buffer size greater than part size`() {
        val first = ByteArray(4) { it.toByte() }
        val second = ByteArray(4) { (it + 4).toByte() }

        val chainedStream = ChainedInputStream(
            first.inputStream(),
            second.inputStream(),
        )

        expectThat(readStream(chainedStream, 5))
            .contentEquals(first + second)
    }

    @Test
    fun `Test reading with buffer size greater than total length`() {
        val first = ByteArray(4) { it.toByte() }
        val second = ByteArray(4) { (it + 4).toByte() }

        val chainedStream = ChainedInputStream(
            first.inputStream(),
            second.inputStream(),
        )

        expectThat(readStream(chainedStream, 256))
            .contentEquals(first + second)
    }

    @Test
    fun `Test reset`() {
        val first = ByteArray(4) { it.toByte() }
        val second = ByteArray(4) { (it + 4).toByte() }

        val chainedStream = ChainedInputStream(
            first.inputStream(),
            second.inputStream(),
        )

        val result1 = chainedStream.readAllBytes()
        val result2 = chainedStream.readAllBytes()
        chainedStream.reset()
        val result3 = chainedStream.readAllBytes()

        expect {
            that(result1)
                .describedAs("result after first read")
                .contentEquals(first + second)

            that(result2)
                .describedAs("result after stream was read, but wasn't yet reset")
                .isEmpty()

            that(result3)
                .describedAs("result after reset")
                .contentEquals(first + second)
        }
    }

    @Test
    fun `Test closing before read`() {
        var firstClosed = false
        val first = object : InputStream() {
            override fun read() = -1
            override fun close() {
                firstClosed = true
            }
        }

        var secondClosed = false
        val second = object : InputStream() {
            override fun read() = -1
            override fun close() {
                secondClosed = true
            }
        }

        val chainedStream = ChainedInputStream(first, second)
        chainedStream.close()

        expect {
            that(firstClosed).describedAs("first stream was closed").isTrue()
            that(secondClosed).describedAs("second stream was closed").isTrue()
        }
    }

    @Test
    fun `Test closing after read`() {
        var firstClosed = false
        val first = object : InputStream() {
            override fun read() = -1
            override fun close() {
                firstClosed = true
            }
        }

        var secondClosed = false
        val second = object : InputStream() {
            override fun read() = -1
            override fun close() {
                secondClosed = true
            }
        }

        val chainedStream = ChainedInputStream(first, second)
        chainedStream.readAllBytes()
        chainedStream.close()

        expect {
            that(firstClosed).describedAs("first stream was closed").isTrue()
            that(secondClosed).describedAs("second stream was closed").isTrue()
        }
    }

    private fun readStream(stream: InputStream, bufferSize: Int): ByteArray {
        val result = ArrayList<Byte>()
        val buffer = ByteArray(bufferSize)
        while (true) {
            val read = stream.read(buffer)
            if (read >= 0) {
                result += buffer.take(read)
            } else {
                break
            }
        }

        return result.toByteArray()
    }
}
