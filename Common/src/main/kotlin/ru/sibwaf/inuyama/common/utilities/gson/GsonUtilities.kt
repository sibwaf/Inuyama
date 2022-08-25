package ru.sibwaf.inuyama.common.utilities.gson

import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.Reader

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: Reader): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: JsonElement): T = fromJson(json, T::class.java)
