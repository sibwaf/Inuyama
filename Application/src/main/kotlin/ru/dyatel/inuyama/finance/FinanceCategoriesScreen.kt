package ru.dyatel.inuyama.finance

import android.content.Context
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.topPadding
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.DIM_MEDIUM
import ru.dyatel.inuyama.layout.FinanceCategoryItem
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter

class FinanceCategoriesView(context: Context) : BaseScreenView<FinanceCategoriesScreen>(context) {

    lateinit var recyclerView: RecyclerView
        private set

    lateinit var addButton: Button
        private set

    init {
        nestedScrollView {
            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                    topPadding = DIM_MEDIUM
                }

                recyclerView = recyclerView {
                    lparams(width = matchParent, height = wrapContent)
                    layoutManager = LinearLayoutManager(context)
                }

                addButton = tintedButton(R.string.action_add)
            }
        }
    }

}

class FinanceCategoriesScreen : InuScreen<FinanceCategoriesView>() {

    private val categoryBox by instance<Box<FinanceCategory>>()

    private val adapter = ItemAdapter<FinanceCategoryItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    init {
        fastAdapter.withOnClickListener { _, _, item, _ ->
            navigator.goTo(FinanceCategoryScreen(item.category))
            true
        }
    }

    override fun createView(context: Context): FinanceCategoriesView {
        return FinanceCategoriesView(context).apply {
            recyclerView.adapter = fastAdapter

            addButton.setOnClickListener {
                navigator.goTo(FinanceCategoryScreen(FinanceCategory(name = "")))
            }
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        refresh()
        observeChanges<FinanceCategory>(::refresh)
    }

    private fun refresh() {
        adapter.set(categoryBox.all.map { FinanceCategoryItem(it) })
    }
}