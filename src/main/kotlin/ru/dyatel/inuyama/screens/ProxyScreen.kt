package ru.dyatel.inuyama.screens

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.find
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.ProxyItem
import ru.dyatel.inuyama.layout.uniformTextInput
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.ctx
import ru.dyatel.inuyama.utilities.subscribeFor

class ProxyScreenView(context: Context) : BaseScreenView<ProxyScreen>(context) {

    private companion object {
        val recyclerViewId = View.generateViewId()
    }

    private val recyclerView: RecyclerView

    init {
        nestedScrollView {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                tintedButton(R.string.action_add) {
                    setOnClickListener { screen.createProxy() }
                }

                recyclerView {
                    lparams(width = matchParent, height = wrapContent)

                    id = recyclerViewId
                    layoutManager = LinearLayoutManager(context)

                    isNestedScrollingEnabled = false
                }
            }
        }

        recyclerView = find(recyclerViewId)
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class ProxyScreen : Screen<ProxyScreenView>(), KodeinAware {

    private companion object {
        val hostEditId = View.generateViewId()
        val portEditId = View.generateViewId()
    }

    override val kodein by closestKodein { activity }

    private val boxStore by instance<BoxStore>()
    private val proxyBox by instance<Box<Proxy>>()

    private var boxObserver: DataSubscription? = null

    private val adapter = ItemAdapter<ProxyItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = ProxyScreenView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context?) {
        super.onShow(context)

        reload()
        boxObserver = boxStore
                .subscribeFor<Proxy>()
                .on(AndroidScheduler.mainThread())
                .onlyChanges()
                .observer { reload() }
    }

    override fun onHide(context: Context?) {
        boxObserver?.cancel()
        boxObserver = null

        super.onHide(context)
    }

    private fun reload() {
        val proxies = proxyBox.all.map {
            ProxyItem(it)
        }

        adapter.set(proxies)
    }

    fun createProxy() {
        val view = ctx.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            uniformTextInput {
                id = hostEditId
                hintResource = R.string.hint_host
            }
            uniformTextInput {
                id = portEditId
                hintResource = R.string.hint_port

                inputType = InputType.TYPE_CLASS_NUMBER
            }
        }

        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_add_proxy)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    val host = view.find<EditText>(hostEditId).text.toString()
                    val port = view.find<EditText>(portEditId).text.toString().toInt()
                    proxyBox.put(Proxy(host = host, port = port))
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_proxy)!!

}
