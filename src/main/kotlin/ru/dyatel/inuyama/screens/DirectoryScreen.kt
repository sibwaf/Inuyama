package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.buildFastAdapter
import ru.dyatel.inuyama.layout.DirectoryItem
import ru.dyatel.inuyama.model.Directory

class DirectoryView(context: Context) : BaseScreenView<DirectoryScreen>(context) {

    private val recyclerView: RecyclerView

    init {
        recyclerView = recyclerView {
            lparams(width = matchParent, height = matchParent)

            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class DirectoryScreen : Screen<DirectoryView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val directoryBox by instance<Box<Directory>>()
    private var directoryBoxObserver: DataSubscription? = null

    private val adapter = ItemAdapter<DirectoryItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = DirectoryView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        refresh()

        directoryBoxObserver = directoryBox.store
                .subscribe(Directory::class.java)
                .on(AndroidScheduler.mainThread())
                .onlyChanges()
                .observer { refresh() }
    }

    override fun onHide(context: Context) {
        directoryBoxObserver?.cancel()
        directoryBoxObserver = null

        super.onHide(context)
    }

    private fun refresh() {
        val directories = directoryBox.all
                .mapIndexed { index, item ->
                    DirectoryItem(
                            item,
                            { fastAdapter.notifyAdapterItemChanged(index) },
                            {
                                item.path = it
                                directoryBox.put(item)
                            },
                            { directoryBox.remove(item) }
                    )
                }

        adapter.set(directories)
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_directories)!!

}
