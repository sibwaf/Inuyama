package ru.dyatel.inuyama

import ru.dyatel.inuyama.overseer.UpdateDispatchExecutor

// todo: this whole api is bullshit and needs rework
class UpdateDispatcher {

    interface Transaction {

        fun downloadTorrent(magnet: String, path: String)

        // TODO: more actions
        fun onSuccess(action: () -> Unit)
    }

    class TransactionImpl : Transaction {

        val torrents = mutableListOf<Pair<String, String>>()

        var commit: () -> Unit = {}

        override fun downloadTorrent(magnet: String, path: String) {
            torrents += magnet to path
        }

        override fun onSuccess(action: () -> Unit) {
            commit = action
        }

    }

    private val transactions = mutableListOf<TransactionImpl>()

    fun transaction(block: Transaction.() -> Unit) {
        transactions += TransactionImpl().apply(block)
    }

    suspend fun dispatchOn(executor: UpdateDispatchExecutor) {
        for (transaction in transactions) {
            try {
                executor.dispatch(transaction)
            } catch (e: Exception) {
                // TODO: notify
            }
        }
    }
}