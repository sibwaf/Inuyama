package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Gravity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
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
import ru.dyatel.inuyama.layout.FinanceAccountItem
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.components.OptionalView
import sibwaf.inuyama.app.common.components.createOptionalView
import sibwaf.inuyama.app.common.components.withIcon

class FinanceAccountsView(context: Context) : BaseScreenView<FinanceAccountsScreen>(context) {

    lateinit var accountRecyclerView: RecyclerView
        private set

    private lateinit var accountOptionalWrapper: OptionalView

    init {
        coordinatorLayout {
            lparams(width = matchParent, height = matchParent)

            accountRecyclerView = context.recyclerView {
                lparams(width = matchParent, height = wrapContent) {
                    verticalMargin = DIM_MEDIUM
                    horizontalMargin = DIM_LARGE
                }

                layoutManager = LinearLayoutManager(context)
            }

            accountOptionalWrapper = createOptionalView(accountRecyclerView) {
                lparams(width = matchParent, height = wrapContent)
            }

            floatingActionButton {
                withIcon(CommunityMaterial.Icon2.cmd_plus)
                setOnClickListener { screen.createAccount() }
            }.lparams(width = wrapContent, height = wrapContent) {
                margin = DIM_EXTRA_LARGE
                gravity = Gravity.BOTTOM or Gravity.END
            }
        }
    }

    var hasNoAccounts by accountOptionalWrapper::isEmpty
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
        }
    }

    override fun onShow(context: Context?) {
        super.onShow(context)

        val accounts = accountManager.getActiveAccounts() + accountManager.getDisabledAccounts()
        accountAdapter.set(accounts.map { FinanceAccountItem(operationManager, it) })
        view.hasNoAccounts = accounts.isEmpty()
    }

    fun createAccount() {
        navigator.goTo(FinanceAccountScreen(FinanceAccount()))
    }
}
