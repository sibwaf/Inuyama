package ru.sibwaf.inuyama.common.utilities

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object MediaTypes {
    val TEXT_PLAIN = "text/plain".toMediaType()
    val APPLICATION_JSON = "application/json".toMediaType()
    val APPLICATION_OCTET_STREAM = "application/octet-stream".toMediaType()
}

suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) = continuation.resume(response)
        })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ignored: Exception) {
            }
        }
    }
}

fun Response.successOrThrow(): Response {
    if (isSuccessful) {
        return this
    } else {
        throw IOException("HTTP status $code")
    }
}

fun InputStream.asRequestBody(contentType: MediaType): RequestBody {
    return object : RequestBody() {
        override fun contentType() = contentType
        override fun writeTo(sink: BufferedSink) {
            source().use { source -> sink.writeAll(source) }
        }
    }
}
