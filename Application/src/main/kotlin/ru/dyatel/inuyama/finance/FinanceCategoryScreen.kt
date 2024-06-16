package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Gravity
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.scrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.uniformTextInput
import sibwaf.inuyama.app.common.components.withIcon
import sibwaf.inuyama.app.common.utilities.capitalizeSentences

class FinanceCategoryView(context: Context) : BaseScreenView<FinanceCategoryScreen>(context) {

    lateinit var nameEditor: UniformTextInput
        private set

    init {
        coordinatorLayout {
            scrollView {
                lparams(width = matchParent, height = matchParent)

                verticalLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        padding = DIM_EXTRA_LARGE
                    }

                    nameEditor = uniformTextInput {
                        hintResource = R.string.hint_name
                        capitalizeSentences()
                    }
                }
            }

            floatingActionButton {
                withIcon(CommunityMaterial.Icon.cmd_content_save)
                setOnClickListener { screen.save() }
            }.lparams(width = wrapContent, height = wrapContent) {
                margin = DIM_EXTRA_LARGE
                gravity = Gravity.BOTTOM or Gravity.END
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
        }
    }

    fun save() {
        category.name = view.nameEditor.text
        categoryBox.put(category)
        navigator.goBack()
    }
}
