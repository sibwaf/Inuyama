package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Menu
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.wealthfront.magellan.BaseScreenView
import hirondelle.date4j.DateTime
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alignParentBottom
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.alignParentTop
import org.jetbrains.anko.cardview.v7.themedCardView
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.dto.FinanceReceiptInfo
import ru.dyatel.inuyama.finance.dto.FinanceTransferDto
import ru.dyatel.inuyama.finance.dto.TransactionHistoryCursor
import ru.dyatel.inuyama.layout.FinanceAccountItem
import ru.dyatel.inuyama.layout.FinanceReceiptItem
import ru.dyatel.inuyama.layout.FinanceTransferItem
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.model.FinanceTransaction
import ru.dyatel.inuyama.model.FinanceTransfer
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.OptionalView
import sibwaf.inuyama.app.common.components.createOptionalView
import sibwaf.inuyama.app.common.components.floatingActionButton
import sibwaf.inuyama.app.common.components.nestedFloatingActionButton
import sibwaf.inuyama.app.common.components.withIcon
import java.util.TimeZone

class FinanceDashboardView(context: Context) : BaseScreenView<FinanceDashboardScreen>(context) {

    lateinit var accountRecyclerView: RecyclerView
        private set

    lateinit var transactionRecyclerView: RecyclerView
        private set

    private val optionalView: OptionalView

    init {
        val regularView = context.relativeLayout {
            lparams(width = matchParent, height = matchParent)

            val mainView = nestedScrollView {
                id = generateViewId()
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

                            layoutManager = object : LinearLayoutManager(context) {
                                override fun canScrollVertically() = false
                                override fun canScrollHorizontally() = false
                            }
                        }
                    }

                    transactionRecyclerView = recyclerView {
                        lparams(width = matchParent, height = wrapContent)
                        layoutManager = LinearLayoutManager(context)
                    }
                }

                setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    // TODO: rework
                    if (scrollY > oldScrollY && !canScrollVertically(1)) {
                        screen.loadMore()
                    }
                }
            }

            val buttonView = nestedFloatingActionButton {
                id = generateViewId()

                setMainButton {
                    floatingActionButton {
                        withIcon(CommunityMaterial.Icon2.cmd_plus)
                    }
                }

                onMainButtonClick { screen.createEmptyReceipt() }

                addExtraButton {
                    floatingActionButton {
                        withIcon(CommunityMaterial.Icon.cmd_arrow_expand)
                        setOnClickListener { screen.createTransfer() }
                    }
                }
                addExtraButton {
                    floatingActionButton {
                        withIcon(CommunityMaterial.Icon2.cmd_qrcode_scan)
                        setOnClickListener { screen.createReceiptFromQr() }
                    }
                }
            }

            mainView.lparams(width = matchParent) {
                alignParentTop()
            }
            buttonView.lparams {
                margin = DIM_EXTRA_LARGE
                alignParentRight()
                alignParentBottom()
            }
        }

        optionalView = createOptionalView(regularView, true)
    }

    var isEmpty by optionalView::isEmpty
}

class FinanceDashboardScreen : InuScreen<FinanceDashboardView>(), KodeinAware {

    private companion object {
        const val AMOUNT_PER_SCROLL = 32
    }

    override val titleResource = R.string.screen_finance_dashboard

    private val accountManager by instance<FinanceAccountManager>()
    private val operationManager by instance<FinanceOperationManager>()
    private val qrService by instance<FinanceQrService>()

    private val accountAdapter = ItemAdapter<FinanceAccountItem>()
    private val transactionAdapter = ModelAdapter { it: FinanceTransaction ->
        when (it) {
            is FinanceReceipt -> FinanceReceiptItem(operationManager, it)
            is FinanceTransfer -> FinanceTransferItem(it)
        }
    }

    private var transactionHistoryCursor: TransactionHistoryCursor? = null

    private val categoryStore by instance<Box<FinanceCategory>>()

    override fun createView(context: Context): FinanceDashboardView {
        return FinanceDashboardView(context).apply {
            accountRecyclerView.adapter = accountAdapter.buildFastAdapter().apply {
                withOnClickListener { _, _, item, _ ->
                    createEmptyReceipt(item.account)
                    true
                }
                withOnLongClickListener { _, _, item, _ ->
                    navigator.goTo(FinanceAccountScreen(item.account))
                    true
                }
            }

            transactionRecyclerView.adapter = transactionAdapter.buildFastAdapter().apply {
                withOnClickListener { _, _, item, _ ->
                    val screen = when (item) {
                        is FinanceReceiptItem -> FinanceReceiptScreen(item.receipt, false)
                        is FinanceTransferItem -> FinanceTransferScreen(item.transfer)
                    }

                    navigator.goTo(screen)
                    true
                }
            }
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        reloadAccounts()
        observeChanges<FinanceAccount>(::reloadAccounts)

        reloadTransactions()
        observeChanges<FinanceReceipt>(::reloadTransactions)
        observeChanges<FinanceTransfer>(::reloadTransactions)
    }

    private fun reloadAccounts() {
        val accounts = accountManager.getActiveAccounts()

        view.isEmpty = accounts.isEmpty()

        val quickAccessAccounts = accounts.filter { it.quickAccess }
        view.accountRecyclerView.isVisible = quickAccessAccounts.isNotEmpty()
        accountAdapter.set(quickAccessAccounts.map { FinanceAccountItem(operationManager, it) })
    }

    private fun reloadTransactions() {
        transactionHistoryCursor = null
        transactionAdapter.clear()

        loadMore()
    }

    fun loadMore() {
        val (transactions, cursor) = operationManager.getTransactions(count = AMOUNT_PER_SCROLL, cursor = transactionHistoryCursor)
        transactionAdapter.add(transactions)
        transactionHistoryCursor = cursor
    }

    fun createEmptyReceipt(account: FinanceAccount = accountManager.getActiveAccounts().first()) {
        val receiptInfo = FinanceReceiptInfo(
            account = account,
            operations = emptyList(),
            datetime = DateTime.now(TimeZone.getDefault()),
        )

        navigator.goTo(FinanceReceiptScreen(receiptInfo, grabFocus = true))
    }

    fun createReceiptFromQr() {
        launchJob {
            val account = accountManager.getActiveAccounts().first()
            val category = categoryStore.all.first()

            val receipt = try {
                qrService.scanQrIntoReceipt(account, category) ?: return@launchJob
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(view, R.string.message_finance_qr_failed, Snackbar.LENGTH_LONG).show()
                }
                return@launchJob
            }

            withContext(Dispatchers.Main) {
                navigator.goTo(FinanceReceiptScreen(receipt, grabFocus = false))
            }
        }
    }

    fun createTransfer() {
        val accounts = accountManager.getActiveAccounts()

        val transferInfo = FinanceTransferDto(
            fromAccount = accounts[0],
            toAccount = accounts[1],
            amount = 0.0,
            datetime = DateTime.now(TimeZone.getDefault()),
        )

        navigator.goTo(FinanceTransferScreen(transferInfo))
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.goto_finance_accounts).apply {
            isVisible = true
            setOnMenuItemClickListener {
                navigator.goTo(FinanceAccountsScreen())
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
