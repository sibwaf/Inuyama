package ru.dyatel.inuyama.layout.components

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import org.jetbrains.anko._LinearLayout
import org.jetbrains.anko.hintResource
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.utilities.capitalizeSentences

class FinanceAccountEditor(context: Context) : _LinearLayout(context) {

    private val nameEditor: UniformTextInput
    private val initialBalanceEditor: UniformDoubleInput

    val name: String
        get() = nameEditor.text

    val initialBalance: Double
        get() = initialBalanceEditor.value

    init {
        orientation = LinearLayout.VERTICAL

        nameEditor = uniformTextInput {
            hintResource = R.string.hint_name
            capitalizeSentences()
        }

        initialBalanceEditor = uniformDoubleInput {
            hintResource = R.string.hint_finance_account_balance
        }
    }

    fun bindAccountData(account: FinanceAccount) {
        nameEditor.text = account.name
        initialBalanceEditor.value = account.initialBalance
    }

}

inline fun ViewGroup.financeAccountEditor(init: FinanceAccountEditor.() -> Unit = {}): FinanceAccountEditor {
    val view = FinanceAccountEditor(context)
    view.init()
    addView(view)
    return view
}
