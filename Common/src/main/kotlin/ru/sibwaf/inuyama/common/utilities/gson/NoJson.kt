package ru.sibwaf.inuyama.common.utilities.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder

fun GsonBuilder.withNoJsonAnnotationSupport(): GsonBuilder {
    return apply {
        setExclusionStrategies(object : ExclusionStrategy {
            override fun shouldSkipClass(clazz: Class<*>) = false
            override fun shouldSkipField(f: FieldAttributes) = f.getAnnotation(NoJson::class.java) != null
        })
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class NoJson
