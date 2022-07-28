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
import com.wealthfront.magellan.BaseScreenView
import hirondelle.date4j.DateTime
import io.objectbox.Box
import io.objectbox.kotlin.query
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
import ru.dyatel.inuyama.layout.FinanceAccountItem
import ru.dyatel.inuyama.layout.FinanceReceiptItem
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.model.FinanceReceipt_
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

    lateinit var receiptRecyclerView: RecyclerView
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

                    receiptRecyclerView = recyclerView {
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

                onMainButtonClick { screen.createReceipt() }

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
    private val accountFastAdapter = accountAdapter.buildFastAdapter()

    private val categoryStore by instance<Box<FinanceCategory>>()

    private val receiptStore by instance<Box<FinanceReceipt>>()
    private val receiptAdapter = ItemAdapter<FinanceReceiptItem>()
    private val receiptFastAdapter = receiptAdapter.buildFastAdapter()

    private var receiptListOffset = 0

    init {
        accountFastAdapter.withOnClickListener { _, _, item, _ ->
            createReceipt(item.account)
            true
        }
        accountFastAdapter.withOnLongClickListener { _, _, item, _ ->
            navigator.goTo(FinanceAccountScreen(item.account))
            true
        }
        receiptFastAdapter.withOnClickListener { _, _, item, _ ->
            navigator.goTo(FinanceReceiptScreen(item.receipt, grabFocus = false))
            true
        }
    }

    override fun createView(context: Context): FinanceDashboardView {
        return FinanceDashboardView(context).apply {
            accountRecyclerView.adapter = accountFastAdapter
            receiptRecyclerView.adapter = receiptFastAdapter
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        reloadAccounts()
        observeChanges<FinanceAccount>(::reloadAccounts)

        reloadReceipts()
        observeChanges<FinanceReceipt>(::reloadReceipts)
    }

    private fun reloadAccounts() {
        val accounts = accountManager.getActiveAccounts()

        view.isEmpty = accounts.isEmpty()

        val quickAccessAccounts = accounts.filter { it.quickAccess }
        view.accountRecyclerView.isVisible = quickAccessAccounts.isNotEmpty()
        accountAdapter.set(quickAccessAccounts.map { FinanceAccountItem(operationManager, it) })
    }

    private fun reloadReceipts() {
        receiptListOffset = 0
        receiptAdapter.clear()
        loadMore()
    }

    fun loadMore() {
        val receipts = receiptStore
            .query { orderDesc(FinanceReceipt_.datetime) }
            .find(receiptListOffset.toLong(), AMOUNT_PER_SCROLL.toLong())
            .map { FinanceReceiptItem(operationManager, it) }

        receiptListOffset += receipts.size
        receiptAdapter.add(receipts)
    }

    fun createReceipt(account: FinanceAccount? = null) {
        navigator.goTo(
            if (account == null) {
                FinanceReceiptScreen(grabFocus = true)
            } else {
                FinanceReceiptScreen(
                    FinanceReceiptInfo(
                        account = account,
                        operations = emptyList(),
                        datetime = DateTime.now(TimeZone.getDefault()),
                    ),
                    grabFocus = true
                )
            }
        )
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
        navigator.goTo(FinanceTransferScreen())
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
