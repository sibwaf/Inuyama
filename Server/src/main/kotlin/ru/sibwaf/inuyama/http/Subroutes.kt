package ru.sibwaf.inuyama.http

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HandlerType

@DslMarker
annotation class SubrouteDsl

@SubrouteDsl
interface HttpSubroute {
    fun addHandler(handlerType: HandlerType, path: String, handler: (Context) -> Unit)
    fun get(path: String, handler: (Context) -> Unit) = addHandler(HandlerType.GET, path, handler)
    fun post(path: String, handler: (Context) -> Unit) = addHandler(HandlerType.POST, path, handler)
}

private fun Javalin.asSubrouteParent() = object : HttpSubroute {
    override fun addHandler(handlerType: HandlerType, path: String, handler: (Context) -> Unit) {
        this@asSubrouteParent.addHandler(handlerType, path, handler)
    }
}

private fun HttpSubroute.withPrefix(path: String) = object : HttpSubroute {
    private val outerPath = path
    override fun addHandler(handlerType: HandlerType, path: String, handler: (Context) -> Unit) {
        this@withPrefix.addHandler(handlerType, "$outerPath$path", handler)
    }
}

fun HttpSubroute.subroute(path: String, block: HttpSubroute.() -> Unit) = withPrefix(path).block()

fun Javalin.subroute(path: String, block: HttpSubroute.() -> Unit) = asSubrouteParent().subroute(path, block)
