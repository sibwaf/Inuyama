package ru.dyatel.inuyama

import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.utilities.PreferenceHelper

class MigrationRunner(override val kodein: Kodein) : KodeinAware {

    private val store by instance<BoxStore>()
    private val preferenceHelper by instance<PreferenceHelper>()

    @Suppress("deprecation")
    fun migrate() {
        val lastUsedVersion = preferenceHelper.lastUsedVersion
        val currentVersion = BuildConfig.VERSION_CODE

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

        preferenceHelper.lastUsedVersion = currentVersion
        return
    }
}