package com.nailong.accounting.domain.repository

import com.nailong.accounting.domain.model.Budget
import com.nailong.accounting.domain.model.BudgetUsage
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun setTotalBudget(ledgerId: String, period: String, amountInCents: Long)
    suspend fun setCategoryBudget(
        ledgerId: String,
        period: String,
        categoryId: String,
        amountInCents: Long,
    )
    fun observeBudgets(ledgerId: String, period: String): Flow<List<Budget>>
    fun observeTotalBudgetUsage(
        ledgerId: String,
        period: String,
        usedAmountInCents: Long,
    ): Flow<BudgetUsage>
}
