package ru.sibwaf.inuyama.finance

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticService

val financeModule = Kodein.Module("finance") {
    bind<FinanceBackupDataProvider>() with singleton { FinanceBackupDataProvider(instance(), instance()) }

    bind<CurrencyConverter>() with singleton {
        CurrencyConverter(
            exchangeRateHostApi = instance(),
        )
    }

    bind<FinanceAnalyticService>() with singleton { FinanceAnalyticService(instance()) }

    bind<FinanceHttpHandler>() with singleton { FinanceHttpHandler(instance(), instance()) }
}
