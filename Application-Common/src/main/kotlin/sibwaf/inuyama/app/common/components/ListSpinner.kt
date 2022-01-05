package sibwaf.inuyama.app.common.components

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner

open class ListSpinner<T>(context: Context) : AppCompatSpinner(context) {

    private var allowDefaultSelection = false

    private val listAdapter: ArrayAdapter<String>

    private var items = listOf<T>()
    var selected: T?
        get() {
            if (allowDefaultSelection) {
                return selectedItemPosition.takeIf { it != 0 }?.let { items[it - 1] }
            }

            return items.getOrNull(selectedItemPosition)
        }
        set(value) {
            if (allowDefaultSelection) {
                setSelection(items.indexOf(value) + 1)
            } else {
                val index = items.indexOf(value)
                if (index == -1) {
                    throw NoSuchElementException()
                }

                setSelection(index)
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
        allowDefaultSelection = true

        listAdapter.clear()

        listAdapter.add(defaultText)
        listAdapter.addAll(items.map(labelMapper))

        this.items = items.toList()
    }

    fun bindItems(items: List<T>, labelMapper: (T) -> String) {
        allowDefaultSelection = false

        listAdapter.clear()
        listAdapter.addAll(items.map(labelMapper))

        this.items = items.toList()
    }

    fun onItemSelected(listener: (T?) -> Unit) {
        this.listener = listener
    }

}
