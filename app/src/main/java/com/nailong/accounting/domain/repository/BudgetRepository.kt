package com.nailong.accounting.domain.repository

import com.nailong.accounting.domain.model.BudgetUsage
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun setTotalBudget(ledgerId: String, period: String, amountInCents: Long)
    fun observeTotalBudgetUsage(
        ledgerId: String,
        period: String,
        usedAmountInCents: Long,
    ): Flow<BudgetUsage>
}
