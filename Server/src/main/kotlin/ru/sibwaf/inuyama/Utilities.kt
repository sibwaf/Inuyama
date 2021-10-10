package ru.sibwaf.inuyama

import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.http.Context
import java.io.Reader
import java.time.OffsetDateTime
import java.time.ZoneOffset

inline fun <reified T : Exception> Javalin.exception(crossinline handler: (T, Context) -> Unit) {
    exception(T::class.java) { e, ctx -> handler(e, ctx) }
}

inline fun <reified T> Gson.fromJson(text: String): T = fromJson(text, T::class.java)

inline fun <reified T> Gson.fromJson(reader: Reader): T = fromJson(reader, T::class.java)

val systemZoneOffset: ZoneOffset get() = OffsetDateTime.now().offset
