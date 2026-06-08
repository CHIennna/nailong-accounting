package com.nailong.accounting.domain.usecase

import com.nailong.accounting.domain.repository.BudgetRepository

class SetCategoryBudgetUseCase(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(
        ledgerId: String,
        period: String,
        categoryId: String,
        amountInCents: Long,
    ) {
        require(ledgerId.isNotBlank()) { "请选择账本" }
        require(period.isNotBlank()) { "请选择月份" }
        require(categoryId.isNotBlank()) { "请选择分类" }
        require(amountInCents > 0) { "请输入有效预算金额" }
        budgetRepository.setCategoryBudget(ledgerId, period, categoryId, amountInCents)
    }
}
