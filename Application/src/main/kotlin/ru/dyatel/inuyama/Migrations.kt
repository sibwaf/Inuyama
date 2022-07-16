package ru.dyatel.inuyama

import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceAccount_
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceOperation_
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.utilities.PreferenceHelper
import java.util.UUID

class MigrationRunner(override val kodein: Kodein) : KodeinAware {

    private val store by instance<BoxStore>()
    private val preferenceHelper by instance<PreferenceHelper>()

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

        preferenceHelper.lastUsedVersion = currentVersion
    }
}
