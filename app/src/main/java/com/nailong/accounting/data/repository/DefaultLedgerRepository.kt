package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.LedgerDao
import com.nailong.accounting.data.local.entity.LedgerEntity
import com.nailong.accounting.domain.model.Ledger
import com.nailong.accounting.domain.repository.LedgerRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultLedgerRepository(
    private val ledgerDao: LedgerDao,
) : LedgerRepository {
    override fun observeLedgers(): Flow<List<Ledger>> =
        ledgerDao.observeActiveLedgers().map { ledgers -> ledgers.map { it.toDomain() } }

    override suspend fun getDefaultLedger(): Ledger? =
        ledgerDao.getDefault()?.toDomain()

    override suspend fun createLedger(name: String): Ledger {
        val now = System.currentTimeMillis()
        val entity = LedgerEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            icon = "book",
            color = "#4F7CFF",
            isDefault = true,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        ledgerDao.clearDefault(now)
        ledgerDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun setDefaultLedger(ledgerId: String) {
        val now = System.currentTimeMillis()
        ledgerDao.clearDefault(now)
        ledgerDao.markDefault(ledgerId, now)
    }
}
