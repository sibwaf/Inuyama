package ru.sibwaf.inuyama.utilities

import com.google.gson.Gson
import io.javalin.plugin.json.JsonMapper
import io.javalin.plugin.json.PipedStreamUtil
import java.io.InputStream

class JavalinGson(private val gson: Gson) : JsonMapper {

    override fun toJsonString(obj: Any): String {
        return when (obj) {
            is String -> obj
            else -> gson.toJson(obj)
        }
    }

    override fun toJsonStream(obj: Any): InputStream {
        return when (obj) {
            is String -> obj.byteInputStream()
            else -> PipedStreamUtil.getInputStream { gson.toJson(obj, it.writer()) }
        }
    }

    override fun <T : Any> fromJsonString(json: String, targetClass: Class<T>): T {
        return gson.fromJson(json, targetClass)
    }

    override fun <T : Any> fromJsonStream(json: InputStream, targetClass: Class<T>): T {
        return gson.fromJson(json.reader(), targetClass)
    }
}
