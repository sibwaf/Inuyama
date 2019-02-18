package ru.sibwaf.inuyama

import io.javalin.Context
import io.javalin.Javalin

inline fun <reified T : Exception> Javalin.exception(crossinline handler: (T, Context) -> Unit) {
    exception(T::class.java) { e, ctx -> handler(e, ctx) }
}