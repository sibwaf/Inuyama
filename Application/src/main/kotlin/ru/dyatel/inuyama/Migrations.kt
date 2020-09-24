package ru.dyatel.inuyama

import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.FinanceOperation

class MigrationRunner(override val kodein: Kodein) : KodeinAware {

    private val store by instance<BoxStore>()

    @Suppress("deprecation")
    fun migrate() {
        store.runInTx {
            // TODO: that's gonna become unbearably slow, need migration versioning

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
}