package sibwaf.inuyama.app.common.components

import android.app.DatePickerDialog
import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import hirondelle.date4j.DateTime
import ru.sibwaf.inuyama.common.utilities.asDateTime
import ru.sibwaf.inuyama.common.utilities.tryOrNull
import java.text.SimpleDateFormat

class UniformDatePicker(context: Context) : UniformTextInput(context) {

    // todo: this needs to stop
    private val formatter = SimpleDateFormat("dd.MM.yyyy")

    var date: DateTime?
        get() = tryOrNull { formatter.parse(text)?.asDateTime }
        set(value) {
            text = value?.format("DD.MM.YYYY") ?: ""
        }

    init {
        setOnClickListener {
            DatePickerDialog(context).apply {
                date?.let { updateDate(it.year, it.month - 1, it.day) }

                setOnDateSetListener { _, year, month, dayOfMonth ->
                    date = DateTime.forDateOnly(year, month + 1, dayOfMonth)
                }

                show()
            }
        }
    }
}

inline fun ViewGroup.uniformDatePicker(init: TextInputEditText.() -> Unit = {}): UniformDatePicker {
    val view = UniformDatePicker(context)
    view.uniformTextInputEditText {
        inputType = InputType.TYPE_NULL
        isFocusable = false

        // haha hacks go brrrrrr
        setOnClickListener { view.callOnClick() }

        init()
    }
    addView(view)
    return view
}
