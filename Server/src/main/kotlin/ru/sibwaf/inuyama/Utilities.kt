package ru.sibwaf.inuyama

import com.google.gson.Gson
import io.javalin.Context
import io.javalin.Javalin

inline fun <reified T : Exception> Javalin.exception(crossinline handler: (T, Context) -> Unit) {
    exception(T::class.java) { e, ctx -> handler(e, ctx) }
}

inline fun <reified T> Gson.fromJson(text: String): T = fromJson(text, T::class.java)