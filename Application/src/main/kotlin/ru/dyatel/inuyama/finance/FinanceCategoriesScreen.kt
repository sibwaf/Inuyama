package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Gravity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.FinanceCategoryItem
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.components.OptionalView
import sibwaf.inuyama.app.common.components.createOptionalView
import sibwaf.inuyama.app.common.components.withIcon

class FinanceCategoriesView(context: Context) : BaseScreenView<FinanceCategoriesScreen>(context) {

    lateinit var categoryRecyclerView: RecyclerView
        private set

    private lateinit var categoryOptionalWrapper: OptionalView

    init {
        coordinatorLayout {
            lparams(width = matchParent, height = matchParent)

            categoryRecyclerView = context.recyclerView {
                lparams(width = matchParent, height = wrapContent) {
                    verticalMargin = DIM_MEDIUM
                    horizontalMargin = DIM_LARGE
                }

                layoutManager = LinearLayoutManager(context)
            }

            categoryOptionalWrapper = createOptionalView(categoryRecyclerView) {
                lparams(width = matchParent, height = wrapContent)
            }

            floatingActionButton {
                withIcon(CommunityMaterial.Icon2.cmd_plus)
                setOnClickListener { screen.createCategory() }
            }.lparams(width = wrapContent, height = wrapContent) {
                margin = DIM_EXTRA_LARGE
                gravity = Gravity.BOTTOM or Gravity.END
            }
        }
    }

    var hasNoCategories by categoryOptionalWrapper::isEmpty
}

class FinanceCategoriesScreen : InuScreen<FinanceCategoriesView>() {

    override val titleResource = R.string.screen_finance_categories

    private val categoryBox by instance<Box<FinanceCategory>>()

    private val categoryAdapter = ItemAdapter<FinanceCategoryItem>()

    override fun createView(context: Context): FinanceCategoriesView {
        return FinanceCategoriesView(context).apply {
            categoryRecyclerView.adapter = categoryAdapter.buildFastAdapter().also { adapter ->
                adapter.withOnClickListener { _, _, item, _ ->
                    navigator.goTo(FinanceCategoryScreen(item.category))
                    true
                }
            }
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        val categories = categoryBox.all
        categoryAdapter.set(categories.map { FinanceCategoryItem(it) })
        view.hasNoCategories = categories.isEmpty()
    }

    fun createCategory() {
        navigator.goTo(FinanceCategoryScreen(FinanceCategory(name = "")))
    }
}
