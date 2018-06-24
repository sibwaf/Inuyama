package ru.dyatel.inuyama

import android.app.Activity
import android.content.pm.PackageManager
import com.google.gson.Gson

fun Activity.grantPermissions(vararg permissions: String) {
    val required = permissions
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            .takeUnless { it.isEmpty() }
            ?: return

    requestPermissions(required.toTypedArray(), 1)
}

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)
