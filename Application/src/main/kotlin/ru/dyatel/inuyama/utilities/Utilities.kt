package ru.dyatel.inuyama.utilities

import android.app.Activity
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.IItem
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.reactive.SubscriptionBuilder
import org.jetbrains.anko.inputMethodManager
import org.ocpsoft.prettytime.PrettyTime
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

inline fun <reified T> Gson.fromJson(json: JsonElement): T = fromJson(json, T::class.java)

inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

inline fun <reified T> BoxStore.subscribeFor(): SubscriptionBuilder<Class<T>> = subscribe(T::class.java)

fun <I : IItem<*, *>> IAdapter<I>.buildFastAdapter(): FastAdapter<I> = FastAdapter.with(this)

val prettyTime by lazy { PrettyTime(Locale("ru", "RU")) }
