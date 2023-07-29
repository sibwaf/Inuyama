package ru.dyatel.inuyama.finance

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.Menu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter_extensions.scroll.EndlessRecyclerOnScrollListener
import com.wealthfront.magellan.BaseScreenView
import hirondelle.date4j.DateTime
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.cardview.v7.themedCardView
import org.jetbrains.anko.design.appBarLayout
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.dto.FinanceOperationDirection
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
import sibwaf.inuyama.app.common.utilities.setContentPadding
import java.util.TimeZone

class FinanceDashboardView(context: Context) : BaseScreenView<FinanceDashboardScreen>(context) {

    lateinit var createOperationButton: FloatingActionButton
        private set
    lateinit var createTransferButton: FloatingActionButton
        private set

    lateinit var accountRecyclerView: RecyclerView
        private set

    lateinit var transactionRecyclerView: RecyclerView
        private set

    private lateinit var transactionOptionalWrapper: OptionalView

    init {
        coordinatorLayout {
            lparams(width = matchParent, height = matchParent)

            appBarLayout {
                lparams(width = matchParent, height = wrapContent)

                backgroundColor = Color.TRANSPARENT
                outlineProvider = null

                themedCardView {
                    lparams(width = matchParent, height = wrapContent) {
                        margin = DIM_LARGE
                    }

                    setCardBackgroundColor(context.getColor(R.color.color_primary))
                    setContentPadding(DIM_LARGE)

                    accountRecyclerView = recyclerView {
                        layoutManager = object : LinearLayoutManager(context) {
                            override fun canScrollVertically() = false
                            override fun canScrollHorizontally() = false
                        }
                    }
                }
            }

            transactionRecyclerView = context.recyclerView {
                lparams(width = matchParent, height = wrapContent)

                layoutManager = LinearLayoutManager(context)

                addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
                    override fun onLoadMore(currentPage: Int) {
                        screen.loadMoreTransactions()
                    }
                })
            }
            transactionOptionalWrapper = createOptionalView(transactionRecyclerView, isEmpty = false) {
                lparams(width = matchParent, height = wrapContent) {
                    behavior = AppBarLayout.ScrollingViewBehavior()
                }
            }

            nestedFloatingActionButton {
                lparams(width = wrapContent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                    gravity = Gravity.BOTTOM or Gravity.END
                }

                createOperationButton = setMainButton {
                    floatingActionButton {
                        withIcon(CommunityMaterial.Icon2.cmd_plus)
                    }
                }

                onMainButtonClick { screen.createEmptyReceipt() }

                createTransferButton = addExtraButton {
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
        }
    }

    var hasTransactions by transactionOptionalWrapper::isEmpty
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

    private val loadMoreJobId = generateJobId()
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

        val quickAccessAccounts = accounts.filter { it.quickAccess }
        view.accountRecyclerView.isVisible = quickAccessAccounts.isNotEmpty()
        accountAdapter.set(quickAccessAccounts.map { FinanceAccountItem(operationManager, it) })

        view.createOperationButton.isVisible = accounts.isNotEmpty()
        view.createTransferButton.isVisible = accounts.size >= 2
    }

    private fun reloadTransactions() {
        transactionHistoryCursor = null
        transactionAdapter.clear()

        loadMoreTransactions()
    }

    fun loadMoreTransactions() {
        launchJob(id = loadMoreJobId) {
            val (transactions, cursor) = withContext(Dispatchers.Default) {
                operationManager.getTransactions(
                    count = AMOUNT_PER_SCROLL,
                    cursor = transactionHistoryCursor,
                )
            }

            transactionAdapter.add(transactions)
            transactionHistoryCursor = cursor

            view.hasTransactions = transactionAdapter.adapterItemCount == 0
        }
    }

    fun createEmptyReceipt(account: FinanceAccount = accountManager.getActiveAccounts().first()) {
        val receiptInfo = FinanceReceiptInfo(
            direction = FinanceOperationDirection.EXPENSE,
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
            amountFrom = 0.0,
            amountTo = 0.0,
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
