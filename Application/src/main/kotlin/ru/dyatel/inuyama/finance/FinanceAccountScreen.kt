package ru.dyatel.inuyama.finance

import android.content.Context
import android.widget.Button
import androidx.appcompat.widget.SwitchCompat
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.switchCompat
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.textResource
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.UniformDoubleInput
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.uniformDoubleInput
import sibwaf.inuyama.app.common.components.uniformTextInput
import sibwaf.inuyama.app.common.utilities.capitalizeSentences

class FinanceAccountView(context: Context) : BaseScreenView<FinanceAccountScreen>(context) {

    lateinit var nameEditor: UniformTextInput
        private set
    lateinit var initialBalanceEditor: UniformDoubleInput
        private set
    lateinit var quickAccessSwitch: SwitchCompat
        private set
    lateinit var disabledSwitch: SwitchCompat
        private set

    lateinit var saveButton: Button
        private set

    init {
        nestedScrollView {
            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                }

                nameEditor = uniformTextInput {
                    hintResource = R.string.hint_name
                    capitalizeSentences()
                }

                initialBalanceEditor = uniformDoubleInput {
                    hintResource = R.string.hint_finance_account_balance
                }

                quickAccessSwitch = switchCompat {
                    padding = DIM_LARGE
                    textResource = R.string.hint_finance_quick_access
                }

                disabledSwitch = switchCompat {
                    padding = DIM_LARGE
                    textResource = R.string.hint_finance_disabled
                }

                saveButton = tintedButton(R.string.action_save)
            }
        }
    }
}

class FinanceAccountScreen(private val account: FinanceAccount) : InuScreen<FinanceAccountView>() {

    private val existingAccount = account.id != 0L

    private val accountBox by instance<Box<FinanceAccount>>()

    override fun getTitle(context: Context) =
        if (existingAccount) account.name else context.getString(R.string.screen_finance_new_account)

    override fun createView(context: Context) = FinanceAccountView(context).apply {
        nameEditor.text = account.name
        initialBalanceEditor.value = account.initialBalance
        quickAccessSwitch.isChecked = account.quickAccess
        disabledSwitch.isChecked = account.disabled

        saveButton.setOnClickListener {
            account.name = nameEditor.text
            account.initialBalance = initialBalanceEditor.value
            account.quickAccess = quickAccessSwitch.isChecked
            account.disabled = disabledSwitch.isChecked
            accountBox.put(account)

            if (!existingAccount) {
                navigator.goBack()
            }
        }
    }
}
