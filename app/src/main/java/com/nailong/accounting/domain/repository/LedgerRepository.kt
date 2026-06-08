package com.nailong.accounting.domain.repository

import com.nailong.accounting.domain.model.Ledger
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun observeLedgers(): Flow<List<Ledger>>
    suspend fun getDefaultLedger(): Ledger?
    suspend fun setDefaultLedger(ledgerId: String)
}
