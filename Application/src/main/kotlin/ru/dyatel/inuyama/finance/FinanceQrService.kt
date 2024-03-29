package ru.dyatel.inuyama.finance

import hirondelle.date4j.DateTime
import ru.dyatel.inuyama.QrReader
import ru.dyatel.inuyama.finance.dto.FinanceOperationDirection
import ru.dyatel.inuyama.finance.dto.FinanceOperationInfo
import ru.dyatel.inuyama.finance.dto.FinanceReceiptInfo
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.sibwaf.inuyama.common.api.FirstOfdApi
import sibwaf.inuyama.app.common.NetworkManager
import java.util.TimeZone

class FinanceQrService(
    private val qrReader: QrReader,
    private val firstOfdApi: FirstOfdApi,
    private val networkManager: NetworkManager
) {

    suspend fun scanQrIntoReceipt(account: FinanceAccount, category: FinanceCategory): FinanceReceiptInfo? {
        val qrText = qrReader.readQrCode() ?: return null

        val httpClient = networkManager.getHttpClient(trustedOnly = false)
        val items = firstOfdApi.getTicketItemsByQr(qrText, httpClient)

        return FinanceReceiptInfo(
            direction = FinanceOperationDirection.EXPENSE,
            account = account,
            operations = items.map {
                FinanceOperationInfo(
                    category = category,
                    amount = it.price,
                    description = it.name
                )
            },
            datetime = DateTime.now(TimeZone.getDefault()), // todo: from ticket
        )
    }
}
