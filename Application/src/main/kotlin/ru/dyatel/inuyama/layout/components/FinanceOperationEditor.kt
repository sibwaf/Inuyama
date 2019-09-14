package ru.dyatel.inuyama.layout.components

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
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.utilities.isVisible

class FinanceOperationEditor(context: Context) : _CardView(context) {

    companion object {
        private val directionIconId = View.generateViewId()

        const val TAB_EXPENSE = 0
        const val TAB_TRANSFER = 1
        const val TAB_INCOME = 2
    }

    private lateinit var tabView: TabLayout
    var selectedTab = TAB_EXPENSE
        private set(value) {
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

    lateinit var saveButton: Button
        private set

    init {
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

                val expenseTab = createTab(CommunityMaterial.Icon.cmd_arrow_top_right)
                val transferTab = createTab(CommunityMaterial.Icon.cmd_arrow_expand)
                val incomeTab = createTab(CommunityMaterial.Icon.cmd_arrow_bottom_right)

                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab) = Unit
                    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

                    override fun onTabSelected(tab: TabLayout.Tab) {
                        selectedTab = when (tab) {
                            expenseTab -> TAB_EXPENSE
                            transferTab -> TAB_TRANSFER
                            incomeTab -> TAB_INCOME
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

                saveButton = tintedButton(R.string.action_save)
            }
        }

        syncState()
    }

    private fun syncState() {
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