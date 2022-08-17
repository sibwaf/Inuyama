package ru.dyatel.inuyama.finance

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.wealthfront.magellan.BaseScreenView
import hirondelle.date4j.DateTime
import io.objectbox.Box
import io.objectbox.kotlin.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.scrollView
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceOperation_
import ru.dyatel.inuyama.model.FinanceReceipt_
import ru.dyatel.inuyama.model.FinanceTransfer
import ru.dyatel.inuyama.model.FinanceTransfer_
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.greaterOrEqual
import ru.sibwaf.inuyama.common.utilities.minusMonths
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.SP_EXTRA_LARGE
import sibwaf.inuyama.app.common.SP_LARGE
import sibwaf.inuyama.app.common.components.ListSpinner
import java.util.TimeZone
import java.util.TreeMap

data class FinanceStatisticsCategorySummary(val categoryName: String, val amount: Double)

class FinanceStatisticsView(context: Context) : BaseScreenView<FinanceStatisticsScreen>(context) {

    private lateinit var periodSelector: ListSpinner<Int>

    private lateinit var totalSavingsView: TextView
    private lateinit var totalChangeView: TextView
    private lateinit var summaryView: TextView
    private lateinit var dateView: TextView

    init {
        scrollView {
            lparams(width = matchParent, height = matchParent)

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                totalSavingsView = textView {
                    textSize = SP_EXTRA_LARGE
                }.lparams {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = DIM_LARGE
                }

                relativeLayout {
                    lparams(width = matchParent)

                    val periodSelectorId = View.generateViewId()

                    totalChangeView = textView {
                        textSize = SP_LARGE
                    }.lparams {
                        alignParentLeft()
                        leftOf(periodSelectorId)
                        margin = DIM_LARGE
                    }

                    periodSelector = ListSpinner<Int>(context).apply {
                        id = periodSelectorId
                        bindItems(listOf(1, 3, 6, 12)) { context.resources.getQuantityString(R.plurals.common_months, it, it) }
                        selected = 1
                    }.lparams {
                        alignParentRight()
                        centerVertically()
                    }

                    addView(periodSelector)
                }

                dateView = textView().apply {
                    lparams {
                        margin = DIM_MEDIUM
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                }

                summaryView = textView()

                periodSelector.onItemSelected {
                    updateDateView()
                    screen.changePeriod(it!!)
                }
            }
        }

        updateDateView()
    }

    fun setTotalSavings(savings: Map<String, Double>) {
        totalSavingsView.text = savings.asSequence()
            .joinToString("\n") { (currency, amount) -> context.getString(R.string.label_finance_amount, amount, currency) }
    }

    fun setTotalChange(changes: Map<String, Double>) {
        totalChangeView.text = changes.asSequence()
            .joinToString("\n") { (currency, amount) -> context.getString(R.string.label_finance_change, amount, currency) }
    }

    fun setCategoryList(operations: Map<String, List<FinanceStatisticsCategorySummary>>) {
        summaryView.text = buildString {
            for ((currency, categorySummaries) in operations) {
                for (categorySummary in categorySummaries) {
                    val amountText = context.getString(R.string.label_finance_change, categorySummary.amount, currency)
                    appendLine("${categorySummary.categoryName}: $amountText")
                }

                appendLine()
            }
        }.trimEnd()
    }

    private fun updateDateView() {
        val now = DateTime.now(TimeZone.getDefault()).truncate(DateTime.Unit.DAY)
        val start = now.minusMonths(periodSelector.selected!! - 1)
            .startOfMonth
            .truncate(DateTime.Unit.DAY)

        @SuppressLint("SetTextI18n")
        dateView.text = "$start - $now"
    }
}

class FinanceStatisticsScreen : InuScreen<FinanceStatisticsView>() {

    override val titleResource: Int
        get() = R.string.screen_finance_statistics

    private val accountBox by instance<Box<FinanceAccount>>()
    private val operationBox by instance<Box<FinanceOperation>>()
    private val transferBox by instance<Box<FinanceTransfer>>()

    private val updateJobId = generateJobId()

    private var period = 1

    override fun createView(context: Context): FinanceStatisticsView {
        return FinanceStatisticsView(context)
    }

    override fun onShow(context: Context) {
        update()
    }

    fun changePeriod(period: Int) {
        this.period = period
        update()
    }

    private fun update() {
        val period = period
        launchJob(Dispatchers.Default, id = updateJobId, replacing = true) {
            // todo: migrate to FinanceAccountManager
            val totalSavings = accountBox.all
                .groupingBy { it.currency }
                .aggregateTo(TreeMap()) { _, acc: Double?, it, _ -> (acc ?: 0.0) + it.balance + it.initialBalance }

            withContext(Dispatchers.Main) {
                view.setTotalSavings(totalSavings)
            }

            val start = DateTime.now(TimeZone.getDefault())
                .minusMonths(period - 1)
                .truncate(DateTime.Unit.MONTH)

            val operationsByCurrency = operationBox
                .query {
                    link(FinanceOperation_.receipt)
                        .greaterOrEqual(FinanceReceipt_.datetime, start)
                }
                .findLazy()
                .groupBy { it.receipt.target.account.target.currency }

            val transferAmountByCurrency = transferBox
                .query { greaterOrEqual(FinanceTransfer_.datetime, start) }
                .findLazy()
                .asSequence()
                .flatMap {
                    sequenceOf(
                        it.from.target.currency to -it.amount,
                        it.to.target.currency to +it.amountTo,
                    )
                }
                .groupingBy { (currency, _) -> currency }
                .aggregate { _, acc: Double?, (_, amount), _ -> (acc ?: 0.0) + amount }

            val totalChange = TreeMap<String, Double>().apply {
                for ((currency, operations) in operationsByCurrency) {
                    val amount = operations.sumOf { it.amount }
                    compute(currency) { _, value -> (value ?: 0.0) + amount }
                }

                for ((currency, amount) in transferAmountByCurrency) {
                    compute(currency) { _, value -> (value ?: 0.0) + amount }
                }
            }

            withContext(Dispatchers.Main) {
                view.setTotalChange(totalChange)
            }

            val categorySummariesByCurrency = operationsByCurrency
                .mapValuesTo(TreeMap()) { (_, operations) ->
                    operations
                        .groupingBy { it.categories.first() }
                        .aggregate { _, acc: Double?, it, _ -> (acc ?: 0.0) + it.amount }
                        .asSequence()
                        .map { (category, amount) ->
                            FinanceStatisticsCategorySummary(
                                categoryName = category.name,
                                amount = amount,
                            )
                        }
                        .filter { it.amount != 0.0 }
                        .sortedByDescending { it.amount }
                        .toList()
                }

            withContext(Dispatchers.Main) {
                view.setCategoryList(categorySummariesByCurrency)
            }
        }
    }
}
