package ru.dyatel.inuyama.screens

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.ProxyItem
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.utilities.buildFastAdapter
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.UniformIntegerInput
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.uniformIntegerInput
import sibwaf.inuyama.app.common.components.uniformTextInput

class ProxyScreenView(context: Context) : BaseScreenView<ProxyScreen>(context) {

    private companion object {
        val recyclerViewId = View.generateViewId()
    }

    private lateinit var recyclerView: RecyclerView

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

                recyclerView = recyclerView {
                    lparams(width = matchParent, height = wrapContent)

                    id = recyclerViewId
                    layoutManager = LinearLayoutManager(context)

                    isNestedScrollingEnabled = false
                }
            }
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class ProxyScreen : InuScreen<ProxyScreenView>(), KodeinAware {

    override val titleResource = R.string.screen_proxy

    private val proxyBox by instance<Box<Proxy>>()

    private val adapter = ItemAdapter<ProxyItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = ProxyScreenView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        refresh()
        observeChanges<Proxy>(::refresh)
    }

    private fun refresh() {
        adapter.set(proxyBox.all.map { ProxyItem(it) })
    }

    fun createProxy() {
        lateinit var hostEdit: UniformTextInput
        lateinit var portEdit: UniformIntegerInput

        val view = context!!.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            hostEdit = uniformTextInput {
                hintResource = R.string.hint_host
            }
            portEdit = uniformIntegerInput {
                hintResource = R.string.hint_port
            }
        }

        AlertDialog.Builder(context!!)
                .setTitle(R.string.dialog_add_proxy)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    val host = hostEdit.text
                    val port = portEdit.value
                    proxyBox.put(Proxy(host = host, port = port))
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

}
