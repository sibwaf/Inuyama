package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.tabs.TabLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.cardview.v7._CardView
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.design.tabLayout
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.imageView
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.components.FinanceAccountSelector
import ru.dyatel.inuyama.layout.components.FinanceCategorySelector
import ru.dyatel.inuyama.layout.components.UniformDoubleInput
import ru.dyatel.inuyama.layout.components.UniformTextInput
import ru.dyatel.inuyama.layout.components.financeAccountSelector
import ru.dyatel.inuyama.layout.components.financeCategorySelector
import ru.dyatel.inuyama.layout.components.uniformDoubleInput
import ru.dyatel.inuyama.layout.components.uniformTextInput
import ru.dyatel.inuyama.utilities.isVisible

class FinanceOperationEditor(context: Context) : _CardView(context) {

    companion object {
        private val directionIconId = View.generateViewId()

        const val TAB_EXPENSE = 0
        const val TAB_TRANSFER = 1
        const val TAB_INCOME = 2
    }

    private lateinit var tabView: TabLayout

    private val tabsToIndices = mutableMapOf<TabLayout.Tab, Int>()
    private val indicesToTabs = mutableMapOf<Int, TabLayout.Tab>()

    var selectedTab: Int
        get() = tabView.selectedTabPosition
        set(value) {
            val tab = indicesToTabs[value] ?: throw IllegalArgumentException("Unknown tab")
            tabView.selectTab(tab)
        }

    var allowTransfers = true
        set(value) {
            field = value
            syncState()
        }

    private lateinit var directionContainer: View

    lateinit var currentAccountSelector: FinanceAccountSelector
        private set

    lateinit var expenseCategorySelector: FinanceCategorySelector
        private set
    lateinit var incomeCategorySelector: FinanceCategorySelector
        private set
    lateinit var targetAccountSelector: FinanceAccountSelector
        private set

    lateinit var amountEditor: UniformDoubleInput
        private set

    lateinit var descriptionEditor: UniformTextInput
        private set

    lateinit var saveButton: Button
        private set

    init {
        verticalLayout {
            lparams(width = matchParent, height = wrapContent)

            tabView = tabLayout {
                lparams(width = matchParent, height = wrapContent)

                fun createTab(index: Int, icon: IIcon) {
                    val drawable = IconicsDrawable(context)
                            .icon(icon)
                            .sizeDp(16)
                            .colorRes(R.color.material_drawer_dark_primary_text)

                    val tab = newTab().setIcon(drawable)

                    tabsToIndices[tab] = index
                    indicesToTabs[index] = tab
                    addTab(tab)
                }

                createTab(TAB_EXPENSE, CommunityMaterial.Icon.cmd_arrow_top_right)
                createTab(TAB_TRANSFER, CommunityMaterial.Icon.cmd_arrow_expand)
                createTab(TAB_INCOME, CommunityMaterial.Icon.cmd_arrow_bottom_right)

                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab) = Unit
                    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
                    override fun onTabSelected(tab: TabLayout.Tab) = syncState()
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

                        currentAccountSelector = financeAccountSelector {
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

                descriptionEditor = uniformTextInput {
                    hintResource = R.string.hint_description
                }.apply {
                    lparams(width = matchParent, height = wrapContent) {
                        horizontalMargin = DIM_LARGE
                    }
                }

                saveButton = tintedButton(R.string.action_save)
            }
        }

        syncState()
    }

    private fun syncState() {
        indicesToTabs[TAB_TRANSFER]!!.view.isVisible = allowTransfers

        if (!allowTransfers) {
            if (selectedTab == TAB_TRANSFER) {
                selectedTab = 0
                return
            }
        }

        descriptionEditor.isVisible = selectedTab != TAB_TRANSFER

        incomeCategorySelector.isVisible = selectedTab == TAB_INCOME
        targetAccountSelector.isVisible = selectedTab == TAB_TRANSFER
        expenseCategorySelector.isVisible = selectedTab == TAB_EXPENSE
    }
}

inline fun ViewGroup.financeOperationEditor(init: FinanceOperationEditor.() -> Unit = {}): FinanceOperationEditor {
    val view = FinanceOperationEditor(context)
    view.init()
    addView(view)
    return view
}