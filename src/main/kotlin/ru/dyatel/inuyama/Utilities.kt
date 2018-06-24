package ru.dyatel.inuyama

import android.app.Activity
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.IItem
import io.objectbox.Box
import io.objectbox.BoxStore

fun Activity.grantPermissions(vararg permissions: String) {
    val required = permissions
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            .takeUnless { it.isEmpty() }
            ?: return

    requestPermissions(required.toTypedArray(), 1)
}

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

fun <I : IItem<*, *>> IAdapter<I>.buildFastAdapter(): FastAdapter<I> = FastAdapter.with(this)
