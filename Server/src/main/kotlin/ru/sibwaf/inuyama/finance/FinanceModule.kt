package ru.sibwaf.inuyama.finance

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticService
import ru.sibwaf.inuyama.finance.rates.CrossExchangeRateProvider
import ru.sibwaf.inuyama.finance.rates.ExchangeRateHostExchangeRateProvider
import ru.sibwaf.inuyama.finance.rates.ExchangeRateProvider
import ru.sibwaf.inuyama.finance.rates.MemoryCachedExchangeRateProvider
import java.time.Duration

val financeModule = Kodein.Module("finance") {
    bind<FinanceBackupDataProvider>() with singleton { FinanceBackupDataProvider(instance(), instance()) }

    bind<ExchangeRateProvider>() with singleton {
        MemoryCachedExchangeRateProvider(
            delegate = CrossExchangeRateProvider(
                mainCurrency = "USD",
                delegate = ExchangeRateHostExchangeRateProvider(
                    api = instance(),
                ),
            ),
            cacheExpiration = Duration.ofMinutes(30),
        )
    }

    bind<FinanceAnalyticService>() with singleton {
        FinanceAnalyticService(
            dataProvider = instance(),
            exchangeRateProvider = instance(),
        )
    }

    bind<FinanceHttpHandler>() with singleton { FinanceHttpHandler(instance(), instance()) }
}
