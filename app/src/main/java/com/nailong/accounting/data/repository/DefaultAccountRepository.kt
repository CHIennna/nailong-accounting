package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.AccountDao
import com.nailong.accounting.domain.model.Account
import com.nailong.accounting.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultAccountRepository(
    private val accountDao: AccountDao,
) : AccountRepository {
    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeActiveAccounts().map { accounts -> accounts.map { it.toDomain() } }

    override suspend fun getDefaultAccount(): Account? =
        accountDao.getDefault()?.toDomain()
}
