package ru.sibwaf.inuyama.http

import io.javalin.http.Context

interface HttpFilter {

    fun before(ctx: Context) = Unit

    fun after(ctx: Context) = Unit
}

class InterruptFilterChainException : RuntimeException()
