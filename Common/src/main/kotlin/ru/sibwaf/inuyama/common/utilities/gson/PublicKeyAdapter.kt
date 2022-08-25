package ru.sibwaf.inuyama.common.utilities.gson

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import ru.sibwaf.inuyama.common.utilities.Encoding
import java.security.PublicKey

fun GsonBuilder.registerPublicKeyAdapter(): GsonBuilder {
    return apply {
        registerTypeAdapter(PublicKey::class.java, PublicKeyGsonTypeAdapter().nullSafe())
    }
}

class PublicKeyGsonTypeAdapter : TypeAdapter<PublicKey>() {

    override fun write(writer: JsonWriter, value: PublicKey) {
        writer.value(Encoding.encodeBase64(Encoding.encodeRSAPublicKey(value)))
    }

    override fun read(reader: JsonReader): PublicKey {
        return Encoding.decodeRSAPublicKey(Encoding.decodeBase64(reader.nextString()))
    }
}
