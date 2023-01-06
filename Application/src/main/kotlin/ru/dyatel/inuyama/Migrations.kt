package ru.dyatel.inuyama

import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceAccount_
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceOperation_
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.model.FinanceTransfer
import ru.dyatel.inuyama.model.FinanceTransfer_
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.resaveAll
import java.util.UUID

class MigrationRunner(
    private val store: BoxStore,
    private val preferenceHelper: PreferenceHelper,
) {

    @Suppress("deprecation")
    fun migrate() {
        val lastUsedVersion = preferenceHelper.lastUsedVersion
        val currentVersion = BuildConfig.VERSION_CODE

        if (lastUsedVersion < 0) {
            preferenceHelper.lastUsedVersion = currentVersion
            return
        }

        if (lastUsedVersion == currentVersion) {
            return
        }

        if (lastUsedVersion < 3) {
            store.runInTx {
                val financeOperationBox = store.boxFor<FinanceOperation>()
                financeOperationBox.query {
                    filter { !it.category.isNull }
                }.forEach {
                    it.categories.add(it.category.target!!)
                    it.category.target = null
                    financeOperationBox.put(it)
                }
            }
        }

        if (lastUsedVersion < 5) {
            store.runInTx {
                val financeOperationBox = store.boxFor<FinanceOperation>()
                val financeReceiptBox = store.boxFor<FinanceReceipt>()

                financeOperationBox
                    .query { filter { it.receipt.isNull } }
                    .forEach {
                        val receipt = FinanceReceipt(datetime = it.datetime)
                        receipt.account = it.account
                        receipt.operations.add(it)
                        financeReceiptBox.put(receipt)
                    }
            }
        }

        if (lastUsedVersion < 6) {
            store.runInTx {
                val financeOperationBox = store.boxFor<FinanceOperation>()

                val orphanedOperations = financeOperationBox
                    .query { filter { it.receipt.isNull } }
                    .find()

                financeOperationBox.remove(orphanedOperations)
            }
        }

        if (lastUsedVersion < 7) {
            store.runInTx {
                val financeOperationBox = store.boxFor<FinanceOperation>()

                financeOperationBox
                    .query { isNull(FinanceOperation_.guid) }
                    .forEach {
                        it.guid = UUID.randomUUID()
                        financeOperationBox.put(it)
                    }
            }
        }

        if (lastUsedVersion < 9) {
            store.runInTx {
                val financeAccountBox = store.boxFor<FinanceAccount>()

                financeAccountBox
                    .query { isNull(FinanceAccount_.quickAccess) }
                    .forEach {
                        it.quickAccess = true
                        financeAccountBox.put(it)
                    }

                financeAccountBox
                    .query { isNull(FinanceAccount_.disabled) }
                    .forEach {
                        it.disabled = false
                        financeAccountBox.put(it)
                    }
            }
        }

        if (lastUsedVersion < 10) {
            store.runInTx {
                val financeAccountBox = store.boxFor<FinanceAccount>()
                financeAccountBox
                    .query { isNull(FinanceAccount_.currency) }
                    .forEach {
                        it.currency = ""
                        financeAccountBox.put(it)
                    }
            }
            store.runInTx {
                val financeTransferBox = store.boxFor<FinanceTransfer>()
                financeTransferBox
                    .query { isNull(FinanceTransfer_.amountTo) }
                    .forEach {
                        it.amountTo = it.amount
                        financeTransferBox.put(it)
                    }
            }
        }

        if (lastUsedVersion < 11) {
            store.boxFor<FinanceOperation>().resaveAll()
            store.boxFor<FinanceReceipt>().resaveAll()
            store.boxFor<FinanceTransfer>().resaveAll()
            store.boxFor<NyaaTorrent>().resaveAll()
            store.boxFor<NyaaWatch>().resaveAll()
            store.boxFor<RuranobeVolume>().resaveAll()
        }

        preferenceHelper.lastUsedVersion = currentVersion
    }
}
