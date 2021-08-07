package ru.sibwaf.inuyama.web

import io.javalin.Javalin

interface WebHandler {

    val insecurePaths: Set<String> get() = emptySet()

    fun install(javalin: Javalin)
}
