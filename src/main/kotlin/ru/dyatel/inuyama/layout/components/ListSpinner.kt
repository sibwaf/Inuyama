package ru.dyatel.inuyama.layout.components

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.Proxy

open class ListSpinner<T>(context: Context) : AppCompatSpinner(context) {

    private val listAdapter: ArrayAdapter<String>

    private var items = listOf<T>()
    var selected: T?
        get() = selectedItemPosition.takeIf { it != 0 }?.let { items[it - 1] }
        set(value) {
            if (value == null) {
                setSelection(0)
            } else {
                setSelection(items.indexOf(value) + 1)
            }
        }

    private var listener: ((T?) -> Unit)? = null

    init {
        listAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        adapter = listAdapter

        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                listener?.invoke(selected)
            }
        }
    }

    fun bindItems(items: List<T>, defaultTextResource: Int, labelMapper: (T) -> String) {
        bindItems(items, context.getString(defaultTextResource), labelMapper)
    }

    fun bindItems(items: List<T>, defaultText: String, labelMapper: (T) -> String) {
        listAdapter.clear()

        listAdapter.add(defaultText)
        listAdapter.addAll(items.map(labelMapper))

        this.items = items.toList()
    }

    fun onItemSelected(listener: (T?) -> Unit) {
        this.listener = listener
    }

}

class DirectorySelector(context: Context) : ListSpinner<Directory>(context) {

    fun bindItems(items: List<Directory>) {
        bindItems(items, R.string.const_directory_default) { it.path }
    }

}

fun ViewGroup.directorySelector(init: DirectorySelector.() -> Unit = {}): DirectorySelector {
    val view = DirectorySelector(context)
    view.init()
    addView(view)
    return view
}

class ProxySelector(context: Context) : ListSpinner<Proxy>(context) {

    fun bindItems(items: List<Proxy>) {
        bindItems(items, R.string.const_proxy_default) { "${it.host}:${it.port}" }
    }

}

fun ViewGroup.proxySelector(init: ProxySelector.() -> Unit = {}): ProxySelector {
    val view = ProxySelector(context)
    view.init()
    addView(view)
    return view
}