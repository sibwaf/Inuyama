package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Menu
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.cardview.v7.themedCardView
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.FinanceAccountItem
import ru.dyatel.inuyama.layout.FinanceOperationItem
import ru.dyatel.inuyama.layout.components.OptionalView
import ru.dyatel.inuyama.layout.components.createOptionalView
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter

class FinanceDashboardView(context: Context) : BaseScreenView<FinanceDashboardScreen>(context) {

    lateinit var accountRecyclerView: RecyclerView
        private set

    lateinit var operationRecyclerView: RecyclerView
        private set

    private val optionalView: OptionalView

    init {
        val regularView = context.nestedScrollView {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                themedCardView {
                    lparams(width = matchParent, height = wrapContent) {
                        bottomMargin = DIM_LARGE
                    }

                    setCardBackgroundColor(context.getColor(R.color.color_primary))

                    accountRecyclerView = recyclerView {
                        lparams(width = matchParent, height = wrapContent) {
                            margin = DIM_LARGE
                        }

                        layoutManager = LinearLayoutManager(context)
                    }
                }

                operationRecyclerView = recyclerView {
                    lparams(width = matchParent, height = wrapContent)
                    layoutManager = LinearLayoutManager(context)
                }
            }
        }

        optionalView = createOptionalView(regularView, true)
    }

    var isEmpty by optionalView::isEmpty
}

class FinanceDashboardScreen : InuScreen<FinanceDashboardView>(), KodeinAware {

    override val titleResource = R.string.screen_finance_dashboard

    private val accountStore by instance<Box<FinanceAccount>>()
    private val accountAdapter = ItemAdapter<FinanceAccountItem>()
    private val accountFastAdapter = accountAdapter.buildFastAdapter()

    private val operationStore by instance<Box<FinanceOperation>>()
    private val operationAdapter = ItemAdapter<FinanceOperationItem>()
    private val operationFastAdapter = operationAdapter.buildFastAdapter()

    init {
        accountFastAdapter.withOnClickListener { _, _, item, _ ->
            navigator.goTo(FinanceAccountScreen(item.account))
            true
        }
        operationFastAdapter.withOnClickListener { _, _, item, _ ->
            navigator.goTo(FinanceOperationScreen(item.operation))
            true
        }
    }

    override fun createView(context: Context): FinanceDashboardView {
        return FinanceDashboardView(context).apply {
            accountRecyclerView.adapter = accountFastAdapter
            operationRecyclerView.adapter = operationFastAdapter
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        reloadAccounts()
        observeChanges<FinanceAccount>(::reloadAccounts)

        reloadOperations()
        observeChanges<FinanceOperation>(::reloadOperations)
    }

    private fun reloadAccounts() {
        view.isEmpty = accountStore.isEmpty
        accountStore.all
                .map { FinanceAccountItem(it) }
                .let { accountAdapter.set(it) }
    }

    private fun reloadOperations() {
        operationStore.all
                .sortedByDescending { it.datetime }
                .map { FinanceOperationItem(it) }
                .let { operationAdapter.set(it) }
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.add_finance_account).apply {
            isVisible = true
            setOnMenuItemClickListener {
                navigator.goTo(FinanceAccountScreen(FinanceAccount()))
                true
            }
        }

        menu.findItem(R.id.goto_finance_categories).apply {
            isVisible = true
            setOnMenuItemClickListener {
                navigator.goTo(FinanceCategoriesScreen())
                true
            }
        }
    }
}