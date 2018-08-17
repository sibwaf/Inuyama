package ru.dyatel.inuyama.layout

import android.content.Context
import android.support.v7.widget.AppCompatSpinner
import android.view.ViewGroup
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

    init {
        directoryAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        adapter = directoryAdapter
    }


    fun bindDirectories(directories: List<Directory>) {
        directoryAdapter.clear()

        directoryAdapter.add(context.getString(R.string.const_directory_default))
        directoryAdapter.addAll(directories.map { it.path })

        this.directories = directories.toList()
    }

}

fun ViewGroup.directorySelector(init: DirectorySelector.() -> Unit): DirectorySelector {
    val view = DirectorySelector(context)
    view.init()
    addView(view)
    return view
}
