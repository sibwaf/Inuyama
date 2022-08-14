package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.ViewGroup
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.Proxy
import sibwaf.inuyama.app.common.components.ListSpinner

class DirectorySelector(context: Context) : ListSpinner<Directory>(context) {

    fun bindItems(items: List<Directory>) {
        bindItems(items, R.string.const_directory_default) { it.path }
    }

}

fun ViewGroup.directorySelector(init: DirectorySelector.() -> Unit = {}): DirectorySelector {
    val view = DirectorySelector(context)
    view.init()
    addView(view)
    return view
}

class ProxySelector(context: Context) : ListSpinner<Proxy>(context) {

    fun bindItems(items: List<Proxy>) {
        bindItems(items, R.string.const_proxy_default) { "${it.host}:${it.port}" }
    }

}

fun ViewGroup.proxySelector(init: ProxySelector.() -> Unit = {}): ProxySelector {
    val view = ProxySelector(context)
    view.init()
    addView(view)
    return view
}

class FinanceAccountSelector(context: Context) : ListSpinner<FinanceAccount>(context) {

    fun bindItems(items: List<FinanceAccount>) {
        bindItems(items) {
            val balance = context.getString(R.string.label_finance_amount, it.initialBalance + it.balance, it.currency)
            "${it.name} ($balance)"
        }
    }

}

fun ViewGroup.financeAccountSelector(init: FinanceAccountSelector.() -> Unit = {}): FinanceAccountSelector {
    val view = FinanceAccountSelector(context)
    view.init()
    addView(view)
    return view
}

class FinanceCategorySelector(context: Context) : ListSpinner<FinanceCategory>(context) {

    fun bindItems(items: List<FinanceCategory>) {
        bindItems(items) { it.name }
    }

}

fun ViewGroup.financeCategorySelector(init: FinanceCategorySelector.() -> Unit = {}): FinanceCategorySelector {
    val view = FinanceCategorySelector(context)
    view.init()
    addView(view)
    return view
}
