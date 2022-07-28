package ru.dyatel.inuyama.finance

import io.objectbox.Box
import io.objectbox.kotlin.query
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceAccount_

class FinanceAccountManager(
    private val accountRepository: Box<FinanceAccount>,
) {

    fun getActiveAccounts(): List<FinanceAccount> {
        return accountRepository
            .query { equal(FinanceAccount_.disabled, false) }
            .find()
    }

    fun getDisabledAccounts(): List<FinanceAccount> {
        return accountRepository
            .query { equal(FinanceAccount_.disabled, true) }
            .find()
    }
}
