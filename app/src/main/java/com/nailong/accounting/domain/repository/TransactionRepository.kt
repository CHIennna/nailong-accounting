package com.nailong.accounting.domain.repository

import com.nailong.accounting.domain.model.Transaction
import com.nailong.accounting.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class TransactionInput(
    val ledgerId: String,
    val type: TransactionType,
    val amountInCents: Long,
    val categoryId: String?,
    val accountId: String,
    val targetAccountId: String?,
    val date: Long,
    val note: String,
)

interface TransactionRepository {
    suspend fun addTransaction(input: TransactionInput): String
    suspend fun updateTransaction(transactionId: String, input: TransactionInput)
    suspend fun deleteTransaction(transactionId: String)
    fun observeRecentTransactions(ledgerId: String, limit: Int): Flow<List<Transaction>>
    fun observeTransactionsByDateRange(
        ledgerId: String,
        startAt: Long,
        endAt: Long,
    ): Flow<List<Transaction>>
}
