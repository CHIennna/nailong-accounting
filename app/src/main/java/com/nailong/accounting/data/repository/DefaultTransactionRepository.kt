package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.TransactionDao
import com.nailong.accounting.data.local.entity.TransactionEntity
import com.nailong.accounting.domain.model.Transaction
import com.nailong.accounting.domain.repository.TransactionInput
import com.nailong.accounting.domain.repository.TransactionRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTransactionRepository(
    private val transactionDao: TransactionDao,
) : TransactionRepository {
    override suspend fun addTransaction(input: TransactionInput): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        transactionDao.insert(input.toEntity(id = id, createdAt = now, updatedAt = now))
        return id
    }

    override suspend fun updateTransaction(transactionId: String, input: TransactionInput) {
        val existing = transactionDao.getById(transactionId) ?: return
        transactionDao.update(
            input.toEntity(
                id = transactionId,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteTransaction(transactionId: String) {
        transactionDao.softDelete(transactionId, System.currentTimeMillis())
    }

    override fun observeRecentTransactions(ledgerId: String, limit: Int): Flow<List<Transaction>> =
        transactionDao.observeRecent(ledgerId, limit).map { transactions -> transactions.map { it.toDomain() } }

    private fun TransactionInput.toEntity(
        id: String,
        createdAt: Long,
        updatedAt: Long,
    ): TransactionEntity =
        TransactionEntity(
            id = id,
            ledgerId = ledgerId,
            type = type.value,
            amount = amountInCents,
            categoryId = categoryId,
            accountId = accountId,
            targetAccountId = targetAccountId,
            date = date,
            note = note.trim(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = null,
        )
}
