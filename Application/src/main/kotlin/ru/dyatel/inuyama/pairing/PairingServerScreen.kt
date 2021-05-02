package ru.dyatel.inuyama.pairing

import android.content.Context
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.wealthfront.magellan.BaseScreenView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.StatusBar
import sibwaf.inuyama.app.common.components.statusBar

class PairingServerView(context: Context) : BaseScreenView<PairingServerScreen>(context) {

    lateinit var statusBar: StatusBar
        private set

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent)

            statusBar = statusBar {
                icon = CommunityMaterial.Icon2.cmd_monitor
                textResource = R.string.label_pairing_state
                switchEnabled = true
            }

            nestedScrollView {
                lparams(width = matchParent, height = wrapContent)

                verticalLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        padding = DIM_LARGE
                    }

                    // TODO: server info
                }
            }
        }
    }
}

class PairingServerScreen(private val server: DiscoveredServer) : InuScreen<PairingServerView>() {

    override val titleText = "${server.address}:${server.port}"

    private val pairingManager by instance<PairingManager>()

    override fun createView(context: Context): PairingServerView {
        return PairingServerView(context).apply {
            statusBar.switchState = pairingManager.compareWithPaired(server)

            statusBar.onSwitchChange { state ->
                if (state) {
                    pairingManager.bind(server)
                } else {
                    pairingManager.unbind()
                }
            }
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        launchJob(Dispatchers.Default) {
            while (true) {
                // TODO: check server state
                delay(4000) // TODO: magic constants
            }
        }
    }
}