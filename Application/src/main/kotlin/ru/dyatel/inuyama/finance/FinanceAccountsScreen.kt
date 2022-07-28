package ru.dyatel.inuyama.finance

import android.content.Context
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.textResource
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.FinanceAccountItem
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM

class FinanceAccountsView(context: Context) : BaseScreenView<FinanceAccountsScreen>(context) {

    lateinit var accountRecyclerView: RecyclerView
        private set
    lateinit var createAccountButton: Button
        private set

    init {
        nestedScrollView {
            verticalLayout {
                lparams(width = matchParent, height = wrapContent)

                accountRecyclerView = recyclerView {
                    lparams(width = matchParent, height = wrapContent) {
                        verticalMargin = DIM_MEDIUM
                        horizontalMargin = DIM_LARGE
                    }

                    layoutManager = LinearLayoutManager(context)
                }

                createAccountButton = tintedButton {
                    textResource = R.string.finance_button_add_account
                }
            }
        }
    }
}

class FinanceAccountsScreen : InuScreen<FinanceAccountsView>() {

    override val titleResource = R.string.screen_finance_accounts

    private val accountManager by instance<FinanceAccountManager>()
    private val operationManager by instance<FinanceOperationManager>()

    private val accountAdapter = ItemAdapter<FinanceAccountItem>()

    override fun createView(context: Context): FinanceAccountsView {
        return FinanceAccountsView(context).apply {
            accountRecyclerView.adapter = accountAdapter.buildFastAdapter().also { adapter ->
                adapter.withOnClickListener { _, _, item, _ ->
                    navigator.goTo(FinanceAccountScreen(item.account))
                    true
                }
            }

            createAccountButton.setOnClickListener {
                navigator.goTo(FinanceAccountScreen(FinanceAccount()))
            }
        }
    }

    override fun onShow(context: Context?) {
        super.onShow(context)

        val accounts = accountManager.getActiveAccounts() + accountManager.getDisabledAccounts()
        accountAdapter.set(accounts.map { FinanceAccountItem(operationManager, it) })
    }
}
