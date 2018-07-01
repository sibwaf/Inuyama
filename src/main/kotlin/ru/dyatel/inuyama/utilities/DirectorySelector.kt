package ru.dyatel.inuyama.utilities

import android.widget.ArrayAdapter
import android.widget.Spinner
import ru.dyatel.inuyama.model.Directory

class DirectorySelector(
        private val spinner: Spinner,
        private val directories: List<Directory>,
        initialSelection: Directory? = null
) {

    var directory: Directory?
        get() = spinner.selectedItemPosition.takeIf { it != 0 }?.let { directories[it - 1] }
        set(value) {
            if (value == null) {
                spinner.setSelection(0)
            } else {
                spinner.setSelection(directories.indexOfFirst { it.id == value.id } + 1)
            }
        }

    init {
        spinner.adapter = ArrayAdapter<String>(spinner.context, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            add(context.getString(ru.dyatel.inuyama.R.string.const_directory_default))
            addAll(directories.map { it.path })
        }

        directory = initialSelection
    }

}
