package ru.dyatel.inuyama.utilities

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import io.objectbox.reactive.SubscriptionBuilder
import org.jetbrains.anko.inputMethodManager
import org.ocpsoft.prettytime.PrettyTime
import java.util.Date
import java.util.Locale
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

inline fun <reified T> BoxStore.subscribeFor(): SubscriptionBuilder<Class<T>> = subscribe(T::class.java)

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

fun RecyclerView.propagateTouchEvents() {
    val parent = parent as ViewGroup

    setOnTouchListener { _, event -> parent.onTouchEvent(event)}
    addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
        override fun onTouchEvent(rv: RecyclerView?, e: MotionEvent) {
            parent.onTouchEvent(e)
        }

        override fun onInterceptTouchEvent(rv: RecyclerView?, e: MotionEvent?) = true
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
    })
}

val Date.asDateTime
    get() = DateTime.forInstant(time, TimeZone.getDefault())!!

val DateTime.asDate
    get() = Date(getMilliseconds(TimeZone.getDefault()))

class DateTimeConverter : PropertyConverter<DateTime?, String?> {
    override fun convertToDatabaseValue(entityProperty: DateTime?) = entityProperty?.toString()
    override fun convertToEntityProperty(databaseValue: String?) = databaseValue?.let { DateTime(it) }
}

val prettyTime by lazy { PrettyTime(Locale("ru", "RU")) }
