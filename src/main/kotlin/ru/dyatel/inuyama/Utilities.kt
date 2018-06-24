package ru.dyatel.inuyama

import android.app.Activity
import android.content.pm.PackageManager

fun Activity.grantPermissions(vararg permissions: String) {
    val required = permissions
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            .takeUnless { it.isEmpty() }
            ?: return

    requestPermissions(required.toTypedArray(), 1)
}