package ru.dyatel.inuyama.utilities

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.google.gson.Gson
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.IItem
import com.wealthfront.magellan.Screen
import hirondelle.date4j.DateTime
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.converter.PropertyConverter
import org.jetbrains.anko.inputMethodManager
import java.util.Date
import java.util.TimeZone

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

val Screen<*>.ctx: Context
    get() = getActivity()

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

fun <I : IItem<*, *>> IAdapter<I>.buildFastAdapter(): FastAdapter<I> = FastAdapter.with(this)

fun View.hideIf(condition: Boolean) {
    visibility = if (condition) View.GONE else View.VISIBLE
}

fun EditText.disableUiExtraction() {
    imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
}

fun EditText.disableSuggestions() {
    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
}

val Date.asDateTime
    get() = DateTime.forInstant(time, TimeZone.getDefault())!!

class DateTimeConverter : PropertyConverter<DateTime?, String?> {
    override fun convertToDatabaseValue(entityProperty: DateTime?) = entityProperty?.toString()
    override fun convertToEntityProperty(databaseValue: String?) = databaseValue?.let { DateTime(it) }
}
