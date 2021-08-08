package ru.dyatel.inuyama.overseer

import ru.dyatel.inuyama.UpdateDispatcher
import ru.dyatel.inuyama.pairing.PairedApi

class UpdateDispatchExecutor(private val pairedApi: PairedApi) {

    fun dispatch(transaction: UpdateDispatcher.TransactionImpl) {
        for (torrent in transaction.torrents) {
            pairedApi.downloadTorrent(torrent.first, torrent.second)
        }

        transaction.commit()
    }
}
