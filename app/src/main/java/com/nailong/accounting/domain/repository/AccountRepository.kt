package com.nailong.accounting.domain.repository

import com.nailong.accounting.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun getDefaultAccount(): Account?
    suspend fun createAccount(name: String): Account
}
