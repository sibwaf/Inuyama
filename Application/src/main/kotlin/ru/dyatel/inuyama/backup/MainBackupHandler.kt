package ru.dyatel.inuyama.backup

import com.google.gson.Gson
import io.objectbox.Box
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.model.ProxyBinding
import ru.dyatel.inuyama.overseer.OverseerConfiguration
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import sibwaf.inuyama.app.common.backup.ModuleBackupHandler
import java.io.ByteArrayInputStream
import java.io.InputStream

class MainBackupHandler(
    private val preferenceHelper: PreferenceHelper,
    private val proxyRepository: Box<Proxy>,
    private val proxyBindingRepository: Box<ProxyBinding>,
    private val directoryRepository: Box<Directory>,

    private val gson: Gson
) : ModuleBackupHandler("sibwaf.inuyama") {

    override fun provideData(): InputStream {
        val data = BackupData(
            overseerConfiguration = BackupOverseerConfiguration(
                period = preferenceHelper.overseer.period
            ),
            proxies = proxyRepository.all.map {
                BackupProxy(
                    id = it.id.toString(),
                    host = it.host,
                    port = it.port
                )
            },
            proxyBindings = proxyBindingRepository.all.map {
                BackupProxyBinding(
                    serviceId = it.id.toString(),
                    proxyId = it.proxy.targetId.toString()
                )
            },
            directories = directoryRepository.all.map {
                BackupDirectory(
                    id = it.id.toString(),
                    path = it.path
                )
            }
        )

        val result = Encoding.stringToBytes(gson.toJson(data))
        return ByteArrayInputStream(result)
    }

    override fun restoreData(data: InputStream) {
        val backup = data.reader().use { gson.fromJson<BackupData>(it) }

        preferenceHelper.overseer = OverseerConfiguration(
            period = backup.overseerConfiguration.period
        )

        proxyRepository.store.runInTx {
            proxyBindingRepository.removeAll()
            proxyRepository.removeAll()
            directoryRepository.removeAll()

            val proxyIdMapping = mutableMapOf<String, Long>()
            for (proxy in backup.proxies) {
                proxyIdMapping[proxy.id] = proxyRepository.put(
                    Proxy(
                        host = proxy.host,
                        port = proxy.port
                    )
                )
            }

            for (proxyBinding in backup.proxyBindings) {
                proxyBindingRepository.put(
                    ProxyBinding(
                        id = proxyBinding.serviceId.toLong()
                    ).also {
                        it.proxy.targetId = proxyIdMapping.getValue(proxyBinding.proxyId)
                    }
                )
            }

            for (directory in backup.directories) {
                directoryRepository.put(
                    Directory(
                        path = directory.path
                    )
                )
            }
        }
    }
}

private data class BackupData(
    val overseerConfiguration: BackupOverseerConfiguration,
    val proxies: Collection<BackupProxy>,
    val proxyBindings: Collection<BackupProxyBinding>,
    val directories: Collection<BackupDirectory>
)

private data class BackupOverseerConfiguration(
    val period: Int
)

private data class BackupProxy(
    val id: String,
    val host: String,
    val port: Int
)

private data class BackupProxyBinding(
    val serviceId: String,
    val proxyId: String
)

private data class BackupDirectory(
    val id: String,
    val path: String
)
