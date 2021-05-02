package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.ColorRes
import hirondelle.date4j.DateTime
import org.jetbrains.anko.frameLayout
import ru.dyatel.inuyama.ITEM_TYPE_FINANCE_ACCOUNT
import ru.dyatel.inuyama.ITEM_TYPE_FINANCE_CATEGORY
import ru.dyatel.inuyama.ITEM_TYPE_FINANCE_OPERATION
import ru.dyatel.inuyama.ITEM_TYPE_HOME_UPDATE
import ru.dyatel.inuyama.ITEM_TYPE_NETWORK
import ru.dyatel.inuyama.ITEM_TYPE_PAIRING_SERVER
import ru.dyatel.inuyama.ITEM_TYPE_PROXY
import ru.dyatel.inuyama.ITEM_TYPE_SERVICE_STATE
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.pairing.DiscoveredServer
import ru.sibwaf.inuyama.common.utilities.humanReadable
import sibwaf.inuyama.app.common.RemoteService
import sibwaf.inuyama.app.common.components.UniformSimpleItem
import java.util.TimeZone
import kotlin.math.abs

class RemoteServiceStateItem(val service: RemoteService, @ColorRes var markerColor: Int) : UniformSimpleItem() {
    override fun getRootView(context: Context): ViewGroup = context.frameLayout()

    override val markerColorResource: Int
        get() = markerColor

    override fun getTitle(context: Context) = service.getName(context)
    override fun getType() = ITEM_TYPE_SERVICE_STATE
}

class UpdateItem(val update: Update) : UniformSimpleItem() {
    override fun getRootView(context: Context): ViewGroup = context.frameLayout()

    override fun getHorizontalPadding(context: Context) = 0

    override fun getTitle(context: Context) = update.description
    override fun getSubtitle(context: Context) =
            DateTime.forInstant(update.timestamp, TimeZone.getDefault()).format("DD.MM.YYYY, hh:mm")!!

    override fun getType() = ITEM_TYPE_HOME_UPDATE
}

class ProxyItem(val proxy: Proxy) : UniformSimpleItem() {
    override fun getTitle(context: Context) = "${proxy.host}:${proxy.port}"
    override fun getType() = ITEM_TYPE_PROXY
}

class NetworkItem(val network: Network, trustChangeListener: (Boolean) -> Unit) : UniformSimpleItem() {
    init {
        withIdentifier(network.id)
    }

    override fun getTitle(context: Context) = network.name
    override fun getType() = ITEM_TYPE_NETWORK

    override val switchState = network.trusted
    override val onSwitchStateChange = trustChangeListener
}

class PairingServerItem(val server: DiscoveredServer, paired: Boolean) : UniformSimpleItem() {
    override val markerColorResource = if (paired) R.color.color_ok else R.color.color_pending

    override fun getTitle(context: Context) = server.key.humanReadable
    override fun getSubtitle(context: Context) = "${server.address}:${server.port}"

    override fun getType() = ITEM_TYPE_PAIRING_SERVER
}

class FinanceAccountItem(val account: FinanceAccount) : UniformSimpleItem() {
    override fun getTitle(context: Context) = account.name
    override fun getSubtitle(context: Context) =
            context.getString(R.string.label_finance_amount, account.initialBalance + account.balance)!!

    override fun getType() = ITEM_TYPE_FINANCE_ACCOUNT
}

class FinanceCategoryItem(val category: FinanceCategory) : UniformSimpleItem() {
    override fun getTitle(context: Context) = category.name
    override fun getType() = ITEM_TYPE_FINANCE_CATEGORY
}

class FinanceOperationItem(val operation: FinanceOperation) : UniformSimpleItem() {
    override val markerColorResource = if (operation.amount < 0) R.color.color_fail else R.color.color_ok

    override fun getTitle(context: Context): String {
        val builder = StringBuilder()

        val account = operation.account.target
        val categories = operation.categories.joinToString(", ") { it.name }

        if (categories.isNotEmpty() && operation.amount > 0) {
            builder.append(categories, " > ")
        }

        builder.append(account.name)

        if (categories.isNotEmpty() && operation.amount < 0) {
            builder.append(" > ", categories)
        }

        builder.append(", ", context.getString(R.string.label_finance_amount, abs(operation.amount)))

        return builder.toString()
    }

    override fun getSubtitle(context: Context) = operation.datetime.format("DD.MM.YYYY, hh:mm")!!

    override fun getType() = ITEM_TYPE_FINANCE_OPERATION
}