package sibwaf.inuyama.app.common.components

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.jetbrains.anko.numberPicker
import org.jetbrains.anko.support.v4.ctx
import sibwaf.inuyama.app.common.R
import kotlin.properties.Delegates

class ElementPicker<T>(
    private val editor: EditText,
    private val values: List<T>,
    private val onValueChanged: (T) -> Unit,
    valueConverter: (T) -> String = { it.toString() }
) {

    var currentValue: T
        get() = values[currentIndex]
        set(value) {
            currentIndex = values.indexOf(value).takeIf { it >= 0 }
                ?: throw IllegalArgumentException("Value not found in provided value list")
        }

    private var currentIndex by Delegates.observable(-1) { _, _, index ->
        editor.setText(convertedValues[index])
    }

    private val convertedValues = values.map(valueConverter).toTypedArray()

    init {
        editor.isFocusable = false
    }

    fun showDialog(fragmentManager: FragmentManager) {
        val fragment = ElementPickerFragment()
        fragment.displayedValues = convertedValues
        fragment.selectedValue = currentIndex
        fragment.onValueSelected = {
            if (it != currentIndex) {
                currentIndex = it
                onValueChanged(currentValue)
            }
        }
        fragment.show(fragmentManager, "elementPicker")
    }

}

class ElementPickerFragment : DialogFragment() {

    lateinit var displayedValues: Array<String>
    var selectedValue = -1

    lateinit var onValueSelected: (Int) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = ctx.numberPicker {
            displayedValues = this@ElementPickerFragment.displayedValues
            minValue = 0
            maxValue = displayedValues.size - 1
            value = selectedValue
            wrapSelectorWheel = false
        }

        return AlertDialog.Builder(ctx)
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ -> onValueSelected(layout.value) }
            .setNegativeButton(R.string.action_cancel) { _, _ -> }
            .create()
    }
}