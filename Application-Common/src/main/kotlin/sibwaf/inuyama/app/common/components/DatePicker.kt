package sibwaf.inuyama.app.common.components

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import hirondelle.date4j.DateTime

class DatePicker(private val editor: EditText, date: DateTime) {

    var date: DateTime = date
        set(value) {
            field = value
            editor.setText(value.format("DD.MM.YYYY"))
        }

    init {
        this.date = date // To set editor text
        editor.isFocusable = false
    }

    fun showDialog(fragmentManager: FragmentManager) {
        val fragment = DatePickerFragment()
        fragment.date = date
        fragment.listener = { year, month, day -> date = DateTime.forDateOnly(year, month, day) }
        fragment.show(fragmentManager, "datePicker")
    }

}

class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    lateinit var date: DateTime
    lateinit var listener: (Int, Int, Int) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return DatePickerDialog(context!!, this, date.year, date.month - 1, date.day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        listener.invoke(year, month + 1, dayOfMonth)
    }
}