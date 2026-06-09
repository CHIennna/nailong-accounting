package com.nailong.accounting.domain.repository

import com.nailong.accounting.domain.model.AiAnalysisReport

data class AiCategoryExpenseInput(
    val categoryName: String,
    val amountInCents: Long,
    val percentage: Double,
)

data class AiAnalysisInput(
    val requestId: String,
    val ledgerId: String,
    val ledgerName: String,
    val period: String,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val balanceInCents: Long,
    val budgetInCents: Long?,
    val budgetUsageRate: Double?,
    val transactionCount: Int,
    val categoryExpenses: List<AiCategoryExpenseInput>,
    val dailyAverageExpenseInCents: Long?,
    val topExpenseCategoryName: String?,
)

interface AiAnalysisRepository {
    suspend fun getCachedReport(ledgerId: String, period: String): AiAnalysisReport?
    suspend fun generateReport(input: AiAnalysisInput): AiAnalysisReport
}
