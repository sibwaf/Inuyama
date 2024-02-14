package ru.sibwaf.inuyama.common.utilities.gson

import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.PipedReader
import java.io.PipedWriter
import java.io.Reader

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: Reader): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: JsonElement): T = fromJson(json, T::class.java)

fun Gson.toJsonReader(obj: Any?, scope: CoroutineScope): Reader {
    val result = PipedReader()
    val writer = PipedWriter(result)
    scope.launch {
        writer.use { toJson(obj, it) }
    }
    return result
}
