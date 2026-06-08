package com.nailong.accounting.domain.usecase

import com.nailong.accounting.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: String) {
        transactionRepository.deleteTransaction(transactionId)
    }
}
