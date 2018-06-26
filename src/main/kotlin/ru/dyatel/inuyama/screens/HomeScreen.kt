package ru.dyatel.inuyama.screens

import android.content.Context
import android.view.Menu
import android.view.View
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.find
import org.jetbrains.anko.verticalLayout
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.layout.Marker
import ru.dyatel.inuyama.layout.State
import ru.dyatel.inuyama.layout.marker
import ru.dyatel.inuyama.rutracker.RutrackerApi
import kotlin.properties.Delegates.observable

class HomeView(context: Context) : BaseScreenView<HomeScreen>(context) {

    private companion object {
        val rutrackerMarkerId = View.generateViewId()
    }

    private val rutrackerMarker: Marker

    init {
        verticalLayout {
            marker {
                id = rutrackerMarkerId
                text = context.getString(R.string.module_rutracker)
                state = State.PENDING
            }
        }

        rutrackerMarker = find(rutrackerMarkerId)
    }

    var rutrackerState by observable(State.PENDING) { _, _, value -> rutrackerMarker.state = value }

}

class HomeScreen : Screen<HomeView>(), KodeinAware {

    private class StateChecker(private val service: RemoteService, private val onUpdate: (State) -> Unit) {

        private var coroutine: Job? = null

        fun check() {
            if (coroutine != null) {
                return
            }

            coroutine = launch(UI) {
                onUpdate(State.PENDING)

                val state = async {
                    if (service.checkConnection()) State.OK else State.FAIL
                }.await()

                onUpdate(state)

                coroutine = null
            }
        }

        fun cancel() {
            coroutine?.cancel()
            coroutine = null
        }

    }

    override val kodein by closestKodein { activity }

    private val rutracker by instance<RutrackerApi>()

    private val checkers = mutableListOf<StateChecker>()

    override fun createView(context: Context) = HomeView(context)

    override fun onShow(context: Context) {
        super.onShow(context)

        checkers += StateChecker(rutracker) { state -> view?.rutrackerState = state }

        checkers.forEach { it.check() }
    }

    override fun onHide(context: Context) {
        checkers.forEach { it.cancel() }
        checkers.clear()

        super.onHide(context)
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.refresh).apply {
            isVisible = true
            setOnMenuItemClickListener { checkers.forEach { it.check() }; true }
        }
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_home)!!
}
