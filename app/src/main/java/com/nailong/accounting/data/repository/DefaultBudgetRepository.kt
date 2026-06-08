package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.BudgetDao
import com.nailong.accounting.data.local.entity.BudgetEntity
import com.nailong.accounting.domain.model.BudgetStatus
import com.nailong.accounting.domain.model.BudgetUsage
import com.nailong.accounting.domain.repository.BudgetRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultBudgetRepository(
    private val budgetDao: BudgetDao,
) : BudgetRepository {
    override suspend fun setTotalBudget(ledgerId: String, period: String, amountInCents: Long) {
        require(amountInCents > 0) { "请输入有效预算金额" }
        val now = System.currentTimeMillis()
        val existing = budgetDao.getTotalBudget(ledgerId, period)
        budgetDao.insertOrUpdate(
            BudgetEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                ledgerId = ledgerId,
                categoryId = null,
                period = period,
                amount = amountInCents,
                alertEnabled = true,
                alertThreshold = existing?.alertThreshold ?: 80,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    override fun observeTotalBudgetUsage(
        ledgerId: String,
        period: String,
        usedAmountInCents: Long,
    ): Flow<BudgetUsage> =
        budgetDao.observeByMonth(ledgerId, period).map { budgets ->
            val budget = budgets.firstOrNull { it.categoryId == null }
            val amount = budget?.amount
            val remaining = amount?.minus(usedAmountInCents)
            val rate = amount?.takeIf { it > 0 }?.let { usedAmountInCents.toDouble() / it.toDouble() * 100 }
            val status = when {
                amount == null -> BudgetStatus.NotSet
                usedAmountInCents >= amount -> BudgetStatus.Exceeded
                rate != null && rate >= budget.alertThreshold -> BudgetStatus.Warning
                else -> BudgetStatus.Normal
            }

            BudgetUsage(
                budgetId = budget?.id,
                budgetAmountInCents = amount,
                usedAmountInCents = usedAmountInCents,
                remainingAmountInCents = remaining,
                usageRate = rate,
                status = status,
            )
        }
}
