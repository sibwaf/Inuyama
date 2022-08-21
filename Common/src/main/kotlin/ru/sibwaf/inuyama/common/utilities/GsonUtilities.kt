package ru.sibwaf.inuyama.common.utilities

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import hirondelle.date4j.DateTime

class DateTimeGsonTypeAdapter : TypeAdapter<DateTime>() {

    override fun write(writer: JsonWriter, value: DateTime) {
        writer.value(value.format("YYYY-MM-DDThh:mm:ss"))
    }

    override fun read(reader: JsonReader): DateTime {
        return DateTime(reader.nextString())
    }
}
