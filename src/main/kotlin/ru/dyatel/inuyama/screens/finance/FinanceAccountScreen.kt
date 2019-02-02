package ru.dyatel.inuyama.screens.finance

import android.content.Context
import android.view.Gravity
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.wealthfront.magellan.BaseScreenView
import hirondelle.date4j.DateTime
import io.objectbox.Box
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.design.tabLayout
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.imageView
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.components.FinanceAccountEditor
import ru.dyatel.inuyama.layout.components.FinanceAccountSelector
import ru.dyatel.inuyama.layout.components.FinanceCategorySelector
import ru.dyatel.inuyama.layout.components.UniformDoubleInput
import ru.dyatel.inuyama.layout.components.financeAccountEditor
import ru.dyatel.inuyama.layout.components.financeAccountSelector
import ru.dyatel.inuyama.layout.components.financeCategorySelector
import ru.dyatel.inuyama.layout.components.uniformDoubleInput
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.isVisible
import java.util.TimeZone

private const val TAB_EXPENSE = 0
private const val TAB_TRANSFER = 1
private const val TAB_INCOME = 2
private const val TAB_EDIT = 3

// TODO: support account creation
class FinanceAccountView(context: Context) : BaseScreenView<FinanceAccountScreen>(context) {

    private companion object {
        val directionIconId = View.generateViewId()
    }

    private lateinit var tabView: TabLayout
    private var selectedTab = TAB_EXPENSE
        set(value) {
            field = value
            syncState()
        }

    private lateinit var directionContainer: View

    private lateinit var currentAccount: FinanceAccountSelector

    lateinit var expenseCategorySelector: FinanceCategorySelector
        private set
    lateinit var incomeCategorySelector: FinanceCategorySelector
        private set
    lateinit var targetAccountSelector: FinanceAccountSelector
        private set

    lateinit var amountEditor: UniformDoubleInput
        private set

    lateinit var accountEditor: FinanceAccountEditor
        private set

    init {
        nestedScrollView {
            cardView {
                lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                }

                verticalLayout {
                    lparams(width = matchParent, height = wrapContent)

                    tabView = tabLayout {
                        lparams(width = matchParent, height = wrapContent)

                        fun createTab(icon: IIcon): TabLayout.Tab {
                            val drawable = IconicsDrawable(context)
                                    .icon(icon)
                                    .sizeDp(16)
                                    .colorRes(R.color.material_drawer_dark_primary_text)

                            return newTab().setIcon(drawable).apply { addTab(this) }
                        }

                        val expenseTab = createTab(CommunityMaterial.Icon.cmd_arrow_bottom_right)
                        val transferTab = createTab(CommunityMaterial.Icon.cmd_arrow_expand)
                        val incomeTab = createTab(CommunityMaterial.Icon.cmd_arrow_top_right)
                        val editTab = createTab(CommunityMaterial.Icon2.cmd_pencil)

                        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                            override fun onTabReselected(tab: TabLayout.Tab) = Unit
                            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

                            override fun onTabSelected(tab: TabLayout.Tab) {
                                selectedTab = when (tab) {
                                    expenseTab -> TAB_EXPENSE
                                    transferTab -> TAB_TRANSFER
                                    incomeTab -> TAB_INCOME
                                    editTab -> TAB_EDIT
                                    else -> throw IllegalArgumentException("Unknown tab object")
                                }
                            }
                        })
                    }

                    verticalLayout {
                        lparams(width = matchParent, height = wrapContent) {
                            padding = DIM_LARGE
                        }

                        directionContainer = relativeLayout {
                            lparams(width = matchParent, height = wrapContent) {
                                padding = DIM_LARGE
                            }

                            verticalLayout {
                                incomeCategorySelector = financeCategorySelector {
                                    lparams(width = matchParent, height = wrapContent) {
                                        verticalMargin = DIM_LARGE
                                    }
                                }

                                currentAccount = financeAccountSelector {
                                    lparams(width = matchParent, height = wrapContent) {
                                        verticalMargin = DIM_LARGE
                                    }

                                    isEnabled = false
                                }

                                expenseCategorySelector = financeCategorySelector {
                                    lparams(width = matchParent, height = wrapContent) {
                                        verticalMargin = DIM_LARGE
                                    }
                                }

                                targetAccountSelector = financeAccountSelector {
                                    lparams(width = matchParent, height = wrapContent) {
                                        verticalMargin = DIM_LARGE
                                    }
                                }
                            }.lparams(width = matchParent, height = wrapContent) {
                                alignParentLeft()
                                centerVertically()
                                leftOf(directionIconId)
                            }

                            val icon = IconicsDrawable(context)
                                    .icon(CommunityMaterial.Icon.cmd_arrow_down)
                                    .sizeDp(24)
                                    .colorRes(R.color.material_drawer_dark_primary_text)

                            imageView(icon) {
                                id = directionIconId
                            }.lparams(width = wrapContent, height = wrapContent) {
                                alignParentRight()
                                centerVertically()

                                gravity = Gravity.CENTER_VERTICAL
                                leftMargin = DIM_LARGE
                            }
                        }

                        amountEditor = uniformDoubleInput {
                            hintResource = R.string.hint_finance_amount
                        }.apply {
                            lparams(width = matchParent, height = wrapContent) {
                                horizontalMargin = DIM_LARGE
                            }
                        }

                        accountEditor = financeAccountEditor {
                            lparams(width = matchParent, height = wrapContent) {
                                horizontalMargin = DIM_LARGE
                            }
                        }

                        tintedButton(R.string.action_save) {
                            setOnClickListener { screen.save(selectedTab) }
                        }
                    }
                }
            }
        }

        syncState()
    }

    fun bindAccountData(account: FinanceAccount) {
        accountEditor.bindAccountData(account)
        currentAccount.bindItems(listOf(account))
    }

    private fun syncState() {
        directionContainer.isVisible = selectedTab != TAB_EDIT
        amountEditor.isVisible = selectedTab != TAB_EDIT

        incomeCategorySelector.isVisible = selectedTab == TAB_INCOME
        targetAccountSelector.isVisible = selectedTab == TAB_TRANSFER
        expenseCategorySelector.isVisible = selectedTab == TAB_EXPENSE

        accountEditor.isVisible = selectedTab == TAB_EDIT
    }

}

class FinanceAccountScreen(private val account: FinanceAccount) : InuScreen<FinanceAccountView>(), KodeinAware {

    override val titleText = account.name

    private val accountBox by instance<Box<FinanceAccount>>()
    private val categoryBox by instance<Box<FinanceCategory>>()
    private val operationBox by instance<Box<FinanceOperation>>()

    override fun createView(context: Context) = FinanceAccountView(context).apply {
        targetAccountSelector.bindItems(accountBox.all.filter { it.id != account.id })

        val categories = categoryBox.all
        expenseCategorySelector.bindItems(categories)
        incomeCategorySelector.bindItems(categories)

        bindAccountData(account)
    }

    fun save(tab: Int) {
        when (tab) {
            TAB_EXPENSE -> {
                val amount = view!!.amountEditor.value
                val category = view!!.expenseCategorySelector.selected!!

                account.balance -= amount

                val operation = FinanceOperation(amount = -amount, datetime = DateTime.now(TimeZone.getDefault()))
                operation.account.target = account
                operation.category.target = category

                boxStore.runInTx {
                    accountBox.put(account)
                    operationBox.put(operation)
                }
            }
            TAB_TRANSFER -> {
                val other = view!!.targetAccountSelector.selected!!
                val amount = view!!.amountEditor.value

                account.balance -= amount
                other.balance += amount

                accountBox.put(account, other)
            }
            TAB_INCOME -> {
                val amount = view!!.amountEditor.value
                val category = view!!.incomeCategorySelector.selected!!

                account.balance += amount

                val operation = FinanceOperation(amount = amount, datetime = DateTime.now(TimeZone.getDefault()))
                operation.account.target = account
                operation.category.target = category

                boxStore.runInTx {
                    accountBox.put(account)
                    operationBox.put(operation)
                }
            }
            TAB_EDIT -> {
                account.name = view.accountEditor.name
                account.initialBalance = view.accountEditor.initialBalance
                accountBox.put(account)
            }
            else -> throw IllegalArgumentException("Unknown tab is selected")
        }

        navigator.goBack()
    }
}