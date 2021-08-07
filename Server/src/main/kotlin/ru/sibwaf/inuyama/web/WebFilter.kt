package ru.sibwaf.inuyama.web

import io.javalin.Context

interface WebFilter {

    fun before(ctx: Context) = Unit

    fun after(ctx: Context) = Unit
}

class InterruptFilterChainException : RuntimeException()
