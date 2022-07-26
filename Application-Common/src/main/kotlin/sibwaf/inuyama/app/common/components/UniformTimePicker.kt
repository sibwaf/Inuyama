package sibwaf.inuyama.app.common.components

import android.app.TimePickerDialog
import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import hirondelle.date4j.DateTime
import ru.sibwaf.inuyama.common.utilities.asDateTime
import ru.sibwaf.inuyama.common.utilities.tryOrNull
import java.text.SimpleDateFormat

class UniformTimePicker(context: Context) : UniformTextInput(context) {

    // todo: this needs to stop
    private val formatter = SimpleDateFormat("HH:mm")

    // todo: probably `now` instead of null would be better
    var time: DateTime?
        get() = tryOrNull { formatter.parse(text)?.asDateTime }
        set(value) {
            text = value?.format("hh:mm") ?: ""
        }

    init {
        setOnClickListener {
            TimePickerDialog(
                context,
                { _, hour, minute -> time = DateTime.forTimeOnly(hour, minute, 0, 0) },
                time?.hour ?: -1,
                time?.minute ?: -1,
                true
            ).show()
        }
    }
}

inline fun ViewGroup.uniformTimePicker(init: TextInputEditText.() -> Unit = {}): UniformTimePicker {
    val view = UniformTimePicker(context)
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
