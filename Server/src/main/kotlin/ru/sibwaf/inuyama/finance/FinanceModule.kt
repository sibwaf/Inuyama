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
import ru.sibwaf.inuyama.pairing.DeviceManager

val financeModule = Kodein.Module("finance") {
    bind<FinanceBackupDataProvider>() with singleton { FinanceBackupDataProvider(instance(), instance()) }

    bind<ExchangeRateProvider>() with singleton {
        val deviceManager = instance<DeviceManager>()
        val dataProvider = instance<FinanceBackupDataProvider>()

        CrossExchangeRateProvider(
            mainCurrency = "USD",
            delegate = MemoryCachedExchangeRateProvider(
                delegate = ExchangeRateHostExchangeRateProvider(
                    api = instance(),
                    currencySetProvider = {
                        val devices = deviceManager.listDevices()
                        devices.flatMapTo(HashSet()) { deviceId ->
                            dataProvider.getAccounts(deviceId).map { it.currency }
                        }
                    },
                ),
            )
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
