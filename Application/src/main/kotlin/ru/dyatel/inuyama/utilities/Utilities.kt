package ru.dyatel.inuyama.utilities

import android.app.Activity
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.IItem
import hirondelle.date4j.DateTime
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import io.objectbox.query.QueryBuilder.StringOrder
import io.objectbox.reactive.SubscriptionBuilder
import org.jetbrains.anko.inputMethodManager
import org.ocpsoft.prettytime.PrettyTime
import java.io.Reader
import java.util.Locale

fun Activity.grantPermissions(vararg permissions: String) {
    val required = permissions
        .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        .takeUnless { it.isEmpty() }
        ?: return

    requestPermissions(required.toTypedArray(), 1)
}

fun Activity.hideKeyboard() {
    val view = currentFocus ?: return

    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    view.clearFocus()
}

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: Reader): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJson(json: JsonElement): T = fromJson(json, T::class.java)

inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

inline fun <reified T> BoxStore.subscribeFor(): SubscriptionBuilder<Class<T>> = subscribe(T::class.java)

fun <T> Box<T>.updateAll(update: (T) -> T) {
    store.runInTx {
        for (entity in query {  }.findLazy()) {
            put(update(entity))
        }
    }
}

fun <T> Box<T>.resaveAll() = updateAll { it }

private val dateTimeConverter = DateTimeConverter()

fun <T> QueryBuilder<T>.equal(property: Property<T>, value: DateTime): QueryBuilder<T> {
    return equal(property, dateTimeConverter.convertToDatabaseValue(value)!!, StringOrder.CASE_SENSITIVE)
}

fun <T> QueryBuilder<T>.less(property: Property<T>, value: DateTime): QueryBuilder<T> {
    return less(property, dateTimeConverter.convertToDatabaseValue(value)!!, StringOrder.CASE_SENSITIVE)
}

fun <T> QueryBuilder<T>.lessOrEqual(property: Property<T>, value: DateTime): QueryBuilder<T> {
    return lessOrEqual(property, dateTimeConverter.convertToDatabaseValue(value)!!, StringOrder.CASE_SENSITIVE)
}

fun <T> QueryBuilder<T>.greaterOrEqual(property: Property<T>, value: DateTime): QueryBuilder<T> {
    return greaterOrEqual(property, dateTimeConverter.convertToDatabaseValue(value)!!, StringOrder.CASE_SENSITIVE)
}

fun <I : IItem<*, *>> IAdapter<I>.buildFastAdapter(): FastAdapter<I> = FastAdapter.with(this)

val prettyTime by lazy { PrettyTime(Locale("ru", "RU")) }
