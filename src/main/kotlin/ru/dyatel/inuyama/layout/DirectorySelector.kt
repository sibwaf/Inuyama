package ru.dyatel.inuyama.layout

import android.content.Context
import android.support.v7.widget.AppCompatSpinner
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.Directory

class DirectorySelector(context: Context) : AppCompatSpinner(context) {

    private val directoryAdapter: ArrayAdapter<String>

    private var directories = listOf<Directory>()

    var directory: Directory?
        get() = selectedItemPosition.takeIf { it != 0 }?.let { directories[it - 1] }
        set(value) {
            if (value == null) {
                setSelection(0)
            } else {
                setSelection(directories.indexOfFirst { it.id == value.id } + 1)
            }
        }

    private var listener: ((Directory?) -> Unit)? = null

    init {
        directoryAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        adapter = directoryAdapter

        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) = Unit

            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                listener?.invoke(directory)
            }
        }
    }


    fun bindDirectories(directories: List<Directory>) {
        directoryAdapter.clear()

        directoryAdapter.add(context.getString(R.string.const_directory_default))
        directoryAdapter.addAll(directories.map { it.path })

        this.directories = directories.toList()
    }

    fun onItemSelected(listener: (Directory?) -> Unit) {
        this.listener = listener
    }

}

fun ViewGroup.directorySelector(init: DirectorySelector.() -> Unit): DirectorySelector {
    val view = DirectorySelector(context)
    view.init()
    addView(view)
    return view
}
