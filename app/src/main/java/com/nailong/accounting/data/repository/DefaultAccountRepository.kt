package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.AccountDao
import com.nailong.accounting.data.local.entity.AccountEntity
import com.nailong.accounting.domain.model.Account
import com.nailong.accounting.domain.repository.AccountRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultAccountRepository(
    private val accountDao: AccountDao,
) : AccountRepository {
    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeActiveAccounts().map { accounts -> accounts.map { it.toDomain() } }

    override suspend fun getDefaultAccount(): Account? =
        accountDao.getDefault()?.toDomain()

    override suspend fun createAccount(name: String): Account {
        val now = System.currentTimeMillis()
        val isFirstAccount = accountDao.countActiveAndArchived() == 0
        val entity = AccountEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            type = "cash",
            initialBalance = 0,
            icon = "wallet",
            color = "#2F9E44",
            isDefault = isFirstAccount,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        accountDao.insert(entity)
        return entity.toDomain()
    }
}
