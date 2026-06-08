package com.nailong.accounting.domain.usecase

import com.nailong.accounting.domain.model.TransactionType
import com.nailong.accounting.domain.repository.TransactionInput
import com.nailong.accounting.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: String, input: TransactionInput) {
        validate(input)
        transactionRepository.updateTransaction(transactionId, input)
    }

    private fun validate(input: TransactionInput) {
        require(input.ledgerId.isNotBlank()) { "请选择账本" }
        require(input.amountInCents > 0) { "请输入有效金额" }
        require(input.accountId.isNotBlank()) { "请选择账户" }

        when (input.type) {
            TransactionType.Expense,
            TransactionType.Income,
            -> require(!input.categoryId.isNullOrBlank()) { "请选择分类" }

            TransactionType.Transfer -> {
                require(!input.targetAccountId.isNullOrBlank()) { "请选择转入账户" }
                require(input.accountId != input.targetAccountId) { "请选择不同的转入账户" }
            }
        }
    }
}
