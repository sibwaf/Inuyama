package ru.dyatel.inuyama.pairing

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.cardview.v7.themedCardView
import org.jetbrains.anko.design.appBarLayout
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.PairingServerItem
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.sibwaf.inuyama.common.PingApiResponse
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import ru.sibwaf.inuyama.common.utilities.humanReadable
import ru.sibwaf.inuyama.common.utilities.successOrThrow
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.NetworkManager
import sibwaf.inuyama.app.common.components.OptionalView
import sibwaf.inuyama.app.common.components.UniformIntegerInput
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.createOptionalView
import sibwaf.inuyama.app.common.components.uniformEmptyView
import sibwaf.inuyama.app.common.components.uniformIntegerInput
import sibwaf.inuyama.app.common.components.uniformTextInput
import sibwaf.inuyama.app.common.components.withIcon
import sibwaf.inuyama.app.common.utilities.setContentPadding
import java.net.URL
import java.security.PublicKey
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

class PairingView(context: Context) : BaseScreenView<PairingScreen>(context) {

    lateinit var pairedServerAddressView: UniformTextInput
        private set
    lateinit var discoveryPortView: UniformIntegerInput
        private set

    lateinit var discoveredServersRecyclerView: RecyclerView
        private set

    private lateinit var unpairButton: View

    private lateinit var discoveredServersUntrustedNetworkWrapper: OptionalView

    init {
        coordinatorLayout {
            lparams(width = matchParent, height = matchParent)

            appBarLayout {
                lparams(width = matchParent, height = wrapContent)

                backgroundColor = Color.TRANSPARENT
                outlineProvider = null

                themedCardView {
                    lparams(width = matchParent, height = wrapContent) {
                        margin = DIM_LARGE
                    }

                    setCardBackgroundColor(context.getColor(R.color.color_primary))
                    setContentPadding(DIM_LARGE)

                    verticalLayout {
                        lparams(width = matchParent, height = wrapContent)

                        pairedServerAddressView = uniformTextInput {
                            hintResource = R.string.label_pairing_paired_server_address

                            imeOptions = imeOptions or EditorInfo.IME_ACTION_DONE
                            inputType = InputType.TYPE_TEXT_VARIATION_URI

                            setOnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    pairedServerAddressView.clearFocus()
                                }
                                false
                            }
                        }.apply {
                            defaultHintTextColor = context.getColorStateList(com.mikepenz.materialize.R.color.md_dark_primary_text)
                        }.lparams(width = matchParent, height = wrapContent) {
                            margin = DIM_LARGE
                        }

                        discoveryPortView = uniformIntegerInput {
                            hintResource = R.string.label_pairing_discovery_port

                            imeOptions = imeOptions or EditorInfo.IME_ACTION_DONE

                            setOnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    discoveryPortView.clearFocus()
                                }
                                false
                            }

                            doAfterTextChanged {
                                screen?.updateDiscoveryPort(discoveryPortView.value)
                            }
                        }.apply {
                            defaultHintTextColor = context.getColorStateList(com.mikepenz.materialize.R.color.md_dark_primary_text)
                        }.lparams(width = matchParent, height = wrapContent) {
                            margin = DIM_LARGE
                        }
                    }
                }
            }

            discoveredServersRecyclerView = context.recyclerView {
                lparams(width = matchParent, height = wrapContent)
                layoutManager = LinearLayoutManager(context)
                itemAnimator = null
            }
            discoveredServersUntrustedNetworkWrapper = createOptionalView(
                regularView = discoveredServersRecyclerView,
                emptyView = context.uniformEmptyView(context.getString(R.string.label_current_network_untrusted))
            ) {
                lparams(width = matchParent, height = wrapContent) {
                    behavior = AppBarLayout.ScrollingViewBehavior()
                }
            }

            unpairButton = floatingActionButton {
                withIcon(CommunityMaterial.Icon2.cmd_lan_disconnect)
                setOnClickListener { screen.unpair() }
            }.lparams(width = wrapContent, height = wrapContent) {
                margin = DIM_EXTRA_LARGE
                gravity = Gravity.BOTTOM or Gravity.END
            }
        }
    }

    var isNetworkUntrusted by discoveredServersUntrustedNetworkWrapper::isEmpty
    var isPaired by unpairButton::isVisible
}

class PairingScreen : InuScreen<PairingView>() {

    private companion object {
        const val DISCOVERY_PING_DELAY = 4000L
        const val NO_RESPONSE_STILL_ALIVE_DELAY = 10000L
        const val REFRESH_DELAY = 1000L
    }

    override val titleResource = R.string.module_pairing

    private val networkManager by instance<NetworkManager>()

    private val preferenceHelper by instance<PreferenceHelper>()
    private val pairingManager by instance<PairingManager>()
    private val discoveryService by instance<DiscoveryService>()
    private lateinit var discoverListener: (PairedServer) -> Unit

    private val gson by instance<Gson>()

    private val serverAdapter = ItemAdapter<PairingServerItem>()
    private val serverFastAdapter = serverAdapter.buildFastAdapter()
    private val serverItemIds = mutableMapOf<PairedServer, Long>()

    private val serverToLastSeen = ConcurrentHashMap<PairedServer, Date>()

    init {
        serverFastAdapter.withOnClickListener { _, _, item, _ ->
            pair(item.server)
            return@withOnClickListener true
        }
    }

    override fun createView(context: Context): PairingView {
        return PairingView(context).apply {
            discoveredServersRecyclerView.adapter = serverFastAdapter
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        discoverListener = discoveryService.addListener { server ->
            serverToLastSeen[server] = Date()
        }

        launchJob(Dispatchers.Default) {
            while (isActive) {
                val networkTrusted = networkManager.isNetworkTrusted

                withContext(Dispatchers.Main) {
                    view.isNetworkUntrusted = !networkTrusted
                }

                if (networkTrusted) {
                    discoveryService.sendDiscoverRequest()
                }

                delay(DISCOVERY_PING_DELAY)
            }
        }

        launchJob(Dispatchers.Default) {
            while (isActive) {
                if (networkManager.isNetworkTrusted) {
                    val fixedAddress = normalizeAddress(view.pairedServerAddressView.text)
                    val key = fixedAddress?.let { getServerKey(it) }

                    if (key != null) {
                        val host = fixedAddress.substringBeforeLast(":")
                        val port = fixedAddress.substringAfterLast(":").toInt()

                        val server = PairedServer(
                            host = host,
                            port = port,
                            key = key,
                            wasDiscovered = false,
                        )

                        discoverListener(server)
                    }
                }

                delay(DISCOVERY_PING_DELAY)
            }
        }

        launchJob(Dispatchers.Main) {
            while (isActive) {
                refreshServerList()
                delay(REFRESH_DELAY)
            }
        }

        syncView()
    }

    fun updateDiscoveryPort(port: Int) {
        preferenceHelper.discoveryPort = port
    }

    fun pair(server: PairedServer) {
        pairingManager.bind(server)
        syncView()
    }

    fun unpair() {
        pairingManager.unbind()
        syncView()
    }

    private fun syncView() {
        view.isNetworkUntrusted = !networkManager.isNetworkTrusted
        view.isPaired = pairingManager.isPaired

        val fixedServerAddress = preferenceHelper.pairedServer?.address
        view.pairedServerAddressView.text = if (fixedServerAddress != null) {
            "${fixedServerAddress.host}:${fixedServerAddress.port}"
        } else {
            ""
        }

        view.discoveryPortView.value = preferenceHelper.discoveryPort

        refreshServerList()
    }

    private fun normalizeAddress(address: String): String? {
        if (address.isBlank()) {
            return null
        }

        val withoutProtocol = address.substringAfter("://")
        val withoutTrailingSlash = withoutProtocol.trim().removeSuffix("/")

        val hasPort = withoutTrailingSlash.substringAfterLast(":", "").toIntOrNull() != null
        val normalizedAddress = if (hasPort) {
            withoutTrailingSlash
        } else {
            "$withoutTrailingSlash:80"
        }

        val isValid = try {
            URL("http://$normalizedAddress")
            true
        } catch (e: Exception) {
            false
        }

        return normalizedAddress.takeIf { isValid }
    }

    private suspend fun getServerKey(address: String): PublicKey? {
        return try {
            val request = Request.Builder()
                .get()
                .url("http://$address/paired/ping")
                .build()

            val key = networkManager.getHttpClient(true)
                .newCall(request)
                .await()
                .use {
                    val body = it.successOrThrow().body!!.charStream()

                    val encodedKey = gson.fromJson<PingApiResponse>(body).key
                    Encoding.decodeRSAPublicKey(Encoding.decodeBase64(encodedKey))
                }

            Log.d("PairingScreen", "Got the server key from [$address]")

            key
        } catch (e: Exception) {
            Log.d("PairingScreen", "Failed to get the server key with address [$address]", e)
            null
        }
    }

    private fun refreshServerList() {
        val now = Date().time

        val serverList = serverToLastSeen.keys.sortedBy { it.key.humanReadable }
        val items = serverList.map {
            val id = serverItemIds.getOrPut(it) { (serverItemIds.values.maxOrNull() ?: 0) + 1L }
            PairingServerItem(
                server = it,
                isAlive = now - serverToLastSeen.getValue(it).time < NO_RESPONSE_STILL_ALIVE_DELAY,
                isPaired = pairingManager.compareWithPaired(it),
            ).apply { withIdentifier(id) }
        }

        serverAdapter.set(items)
    }

    override fun onHide(context: Context) {
        discoveryService.removeListener(discoverListener)
        super.onHide(context)
    }
}
