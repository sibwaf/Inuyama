package ru.dyatel.inuyama

import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.transmission.TorrentClient

class UpdateDispatcher(override val kodein: Kodein) : KodeinAware {

    interface Transaction {

        fun downloadTorrent(magnet: String, path: String)
        // TODO: more actions
        fun onSuccess(action: () -> Unit)
    }

    private class TransactionImpl : Transaction {

        val torrents = mutableListOf<Pair<String, String>>()

        var commit: () -> Unit = {}

        override fun downloadTorrent(magnet: String, path: String) {
            torrents += magnet to path
        }

        override fun onSuccess(action: () -> Unit) {
            commit = action
        }

    }

    private val torrentClient by instance<TorrentClient>()

    private val transactions = mutableListOf<TransactionImpl>()

    fun transaction(block: Transaction.() -> Unit) {
        transactions += TransactionImpl().apply(block)
    }

    fun commit() {
        for (transaction in transactions) {
            try {
                for (torrent in transaction.torrents) {
                    torrentClient.download(torrent.first, torrent.second)
                }

                transaction.commit()
            } catch (e: Exception) {
                // TODO: notify
            }
        }
    }

}