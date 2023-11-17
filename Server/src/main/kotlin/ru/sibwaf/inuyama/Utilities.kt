package ru.sibwaf.inuyama

import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.http.Context
import java.io.Reader
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter

inline fun <reified T : Exception> Javalin.exception(crossinline handler: (T, Context) -> Unit) {
    exception(T::class.java) { e, ctx -> handler(e, ctx) }
}

inline fun <reified T> Gson.fromJson(text: String): T = fromJson(text, T::class.java)

inline fun <reified T> Gson.fromJson(reader: Reader): T = fromJson(reader, T::class.java)

inline fun <reified T> Gson.fromJson(path: Path): T = path.bufferedReader().use { fromJson(it, T::class.java) }

fun Gson.toJson(src: Any, path: Path) = path.bufferedWriter().use { toJson(src, it) }

val systemZoneOffset: ZoneOffset get() = OffsetDateTime.now().offset
