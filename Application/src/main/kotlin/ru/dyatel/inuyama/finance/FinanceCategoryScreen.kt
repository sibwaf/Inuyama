package ru.dyatel.inuyama.finance

import android.content.Context
import android.widget.Button
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.uniformTextInput
import sibwaf.inuyama.app.common.utilities.capitalizeSentences

class FinanceCategoryView(context: Context) : BaseScreenView<FinanceCategoryScreen>(context) {

    lateinit var nameEditor: UniformTextInput
        private set

    lateinit var saveButton: Button
        private set

    init {
        nestedScrollView {
            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                nameEditor = uniformTextInput {
                    hintResource = R.string.hint_name
                    capitalizeSentences()
                }

                saveButton = tintedButton(R.string.action_save)
            }
        }
    }

}

class FinanceCategoryScreen(private val category: FinanceCategory) : InuScreen<FinanceCategoryView>() {

    override val titleText = if (category.id == 0L) "" else category.name
    override val titleResource = R.string.screen_finance_new_category

    private val categoryBox by instance<Box<FinanceCategory>>()

    override fun createView(context: Context): FinanceCategoryView {
        return FinanceCategoryView(context).apply {
            nameEditor.text = category.name

            saveButton.setOnClickListener {
                category.name = nameEditor.text
                categoryBox.put(category)
                navigator.goBack()
            }
        }
    }
}