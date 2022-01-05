package ru.dyatel.inuyama.finance

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
import ru.dyatel.inuyama.screens.InuScreen
import ru.sibwaf.inuyama.common.utilities.minusMonths
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.SP_EXTRA_LARGE
import sibwaf.inuyama.app.common.SP_LARGE
import sibwaf.inuyama.app.common.components.ListSpinner
import java.util.TimeZone
import kotlin.math.abs
import kotlin.properties.Delegates

class FinanceStatisticsView(context: Context) : BaseScreenView<FinanceStatisticsScreen>(context) {

    private lateinit var periodSelector: ListSpinner<Int>

    private lateinit var totalSavingsView: TextView
    private lateinit var totalChangeView: TextView
    private lateinit var summaryView: TextView
    private lateinit var dateView: TextView

    var totalSavings: Double by Delegates.observable(0.0) { _, _, value ->
        totalSavingsView.text = context.getString(R.string.label_finance_amount, value)
    }

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
                        bindItems(listOf(1, 4, 6, 12)) { context.resources.getQuantityString(R.plurals.common_months, it, it) }
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

    fun setOperationList(operations: List<Pair<String, Double>>) {
        val changeSum = operations.sumByDouble { it.second }
        totalChangeView.text = context.getString(R.string.label_finance_change, changeSum)

        summaryView.text = operations.joinToString("\n") {
            "${it.first}: ${context.getString(R.string.label_finance_amount, it.second)}"
        }
    }

    fun updateDateView() {
        val now = DateTime.now(TimeZone.getDefault()).truncate(DateTime.Unit.DAY)
        val start = now.minusMonths(periodSelector.selected!! - 1)
            .startOfMonth
            .truncate(DateTime.Unit.DAY)

        dateView.text = "$start - $now"
    }
}

class FinanceStatisticsScreen : InuScreen<FinanceStatisticsView>() {

    override val titleResource: Int
        get() = R.string.screen_finance_statistics

    private val accountBox by instance<Box<FinanceAccount>>()
    private val operationBox by instance<Box<FinanceOperation>>()

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
            val totalSavings = accountBox.all.sumByDouble { it.balance + it.initialBalance }
            withContext(Dispatchers.Main) {
                view.totalSavings = totalSavings
            }

            val start = DateTime.now(TimeZone.getDefault())
                .minusMonths(period - 1)
                .truncate(DateTime.Unit.MONTH)

            val operations = operationBox
                .query { filter { it.receipt.target.datetime >= start } } // todo: very inefficient
                .find()
                .groupBy { it.categories.first() }
                .mapKeys { (key, _) -> key.name }
                .mapValues { (_, value) -> value.sumByDouble { it.amount } }
                .toList()

            val (incomeOperations, expenseOperations) = operations.partition { it.second > 0 }
            val sortedOperations = listOf(incomeOperations, expenseOperations)
                .map { it.sortedByDescending { operation -> abs(operation.second) } }
                .flatten()

            withContext(Dispatchers.Main) {
                view.setOperationList(sortedOperations)
            }
        }
    }
}