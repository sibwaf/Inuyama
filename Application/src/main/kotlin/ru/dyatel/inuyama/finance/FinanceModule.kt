package ru.dyatel.inuyama.finance

import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.model.FinanceTransfer
import ru.dyatel.inuyama.utilities.boxFor
import ru.sibwaf.inuyama.common.api.FirstOfdApi

val financeModule = Kodein.Module("finance") {
    bind<Box<FinanceAccount>>() with singleton { instance<BoxStore>().boxFor() }
    bind<Box<FinanceCategory>>() with singleton { instance<BoxStore>().boxFor() }
    bind<Box<FinanceReceipt>>() with singleton { instance<BoxStore>().boxFor() }
    bind<Box<FinanceOperation>>() with singleton { instance<BoxStore>().boxFor() }
    bind<Box<FinanceTransfer>>() with singleton { instance<BoxStore>().boxFor() }

    bind<FirstOfdApi>() with singleton { FirstOfdApi() }

    bind<FinanceAccountManager>() with singleton { FinanceAccountManager(instance()) }
    bind<FinanceOperationManager>() with singleton {
        FinanceOperationManager(
            boxStore = instance(),
            accountBox = instance(),
            receiptBox = instance(),
            operationBox = instance(),
            transferBox = instance(),
        )
    }
    bind<FinanceQrService>() with provider {
        FinanceQrService(
            qrReader = instance(),
            firstOfdApi = instance(),
            networkManager = instance()
        )
    }

    bind<FinanceBackupHandler>() with singleton {
        FinanceBackupHandler(
            accountRepository = instance(),
            categoryRepository = instance(),
            operationRepository = instance(),
            receiptRepository = instance(),
            transferRepository = instance(),
            gson = instance()
        )
    }
}
