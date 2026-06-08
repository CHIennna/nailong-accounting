package com.nailong.accounting.domain.model

data class Ledger(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val isArchived: Boolean,
)

data class Category(
    val id: String,
    val name: String,
    val type: TransactionType,
    val icon: String,
    val color: String,
    val sortOrder: Int,
)

data class Account(
    val id: String,
    val name: String,
    val type: String,
    val initialBalanceInCents: Long,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
)

data class Transaction(
    val id: String,
    val ledgerId: String,
    val type: TransactionType,
    val amountInCents: Long,
    val categoryId: String?,
    val accountId: String,
    val targetAccountId: String?,
    val date: Long,
    val note: String,
)

data class MonthlySummary(
    val ledgerId: String,
    val period: String,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val balanceInCents: Long,
    val budgetInCents: Long?,
    val budgetUsageRate: Double?,
    val transactionCount: Int,
)

data class BudgetUsage(
    val budgetId: String?,
    val budgetAmountInCents: Long?,
    val usedAmountInCents: Long,
    val remainingAmountInCents: Long?,
    val usageRate: Double?,
    val status: BudgetStatus,
)

data class Budget(
    val id: String,
    val ledgerId: String,
    val categoryId: String?,
    val period: String,
    val amountInCents: Long,
    val alertThreshold: Int,
)

data class CategoryBudgetUsage(
    val categoryId: String,
    val categoryName: String,
    val budgetAmountInCents: Long,
    val usedAmountInCents: Long,
    val remainingAmountInCents: Long,
    val usageRate: Double,
    val status: BudgetStatus,
)

enum class BudgetStatus {
    NotSet,
    Normal,
    Warning,
    Exceeded,
}

enum class TransactionType(val value: String) {
    Expense("expense"),
    Income("income"),
    Transfer("transfer");

    companion object {
        fun fromValue(value: String): TransactionType =
            entries.firstOrNull { it.value == value } ?: error("Unknown transaction type: $value")
    }
}
