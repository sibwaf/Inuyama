package ru.dyatel.inuyama.finance.components

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import hirondelle.date4j.DateTime
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.matchParent
import ru.dyatel.inuyama.utilities.ListenableEditor
import ru.dyatel.inuyama.utilities.PublishListenerHolderImpl
import ru.dyatel.inuyama.utilities.withBatching
import ru.dyatel.inuyama.utilities.withEditor
import ru.sibwaf.inuyama.common.utilities.toDateOnly
import ru.sibwaf.inuyama.common.utilities.toTimeOnly
import ru.sibwaf.inuyama.common.utilities.withTimeFrom
import sibwaf.inuyama.app.common.components.UniformDatePicker
import sibwaf.inuyama.app.common.components.UniformTimePicker
import sibwaf.inuyama.app.common.components.uniformDatePicker
import sibwaf.inuyama.app.common.components.uniformTimePicker

class DateTimeEditor(context: Context) : FrameLayout(context), ListenableEditor<DateTime> {

    private lateinit var datePicker: UniformDatePicker
    private lateinit var timePicker: UniformTimePicker

    private val changePublisher = PublishListenerHolderImpl<DateTime>()
        .withEditor(this)
        .withBatching()

    init {
        linearLayout {
            datePicker = uniformDatePicker {
                gravity = Gravity.CENTER

                doAfterTextChanged { changePublisher.notifyListener() }
            }.lparams(width = matchParent) { weight = 1.0f }

            timePicker = uniformTimePicker {
                gravity = Gravity.CENTER

                doAfterTextChanged { changePublisher.notifyListener() }
            }.lparams(width = matchParent) { weight = 1.0f }
        }
    }

    override fun onChange(listener: (DateTime) -> Unit) = changePublisher.onChange(listener)

    override fun fillFrom(data: DateTime) {
        changePublisher.notifyAfterBatch {
            datePicker.date = data.toDateOnly()
            timePicker.time = data.toTimeOnly()
        }
    }

    override fun buildValue() = datePicker.date!! withTimeFrom timePicker.time!!
}
