package ru.sibwaf.inuyama.common.utilities.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

fun GsonBuilder.withCaseInsensitiveEnums(): GsonBuilder {
    return apply {
        registerTypeAdapterFactory(object : TypeAdapterFactory {
            override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                if (!type.rawType.isEnum) {
                    return null
                }

                return object : TypeAdapter<T>() {
                    override fun write(out: JsonWriter, value: T) {
                        out.value(value.toString().lowercase())
                    }

                    @Suppress("UNCHECKED_CAST")
                    override fun read(`in`: JsonReader): T {
                        val text = `in`.nextString()
                        val value = type.rawType.enumConstants
                            .firstOrNull { it.toString().equals(text, ignoreCase = true) }
                            ?: throw IllegalArgumentException("Unknown ${type.rawType.canonicalName} value: $text")

                        return value as T
                    }
                }.nullSafe()
            }
        })
    }
}
