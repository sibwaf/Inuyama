package ru.sibwaf.inuyama.common.utilities.gson

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.OffsetDateTime

@Suppress("NewApi")
fun GsonBuilder.registerJavaTimeAdapters(): GsonBuilder {
    return apply {
        registerTypeAdapter(LocalDate::class.java, LocalDateGsonTypeAdapter().nullSafe())
        registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeGsonTypeAdapter().nullSafe())
    }
}

@Suppress("NewApi")
class LocalDateGsonTypeAdapter : TypeAdapter<LocalDate>() {

    override fun write(out: JsonWriter, value: LocalDate) {
        out.value(value.toString())
    }

    override fun read(`in`: JsonReader): LocalDate {
        return LocalDate.parse(`in`.nextString())
    }
}

@Suppress("NewApi")
class OffsetDateTimeGsonTypeAdapter : TypeAdapter<OffsetDateTime>() {

    override fun write(out: JsonWriter, value: OffsetDateTime) {
        out.value(value.toString())
    }

    override fun read(`in`: JsonReader): OffsetDateTime {
        return OffsetDateTime.parse(`in`.nextString())
    }
}
