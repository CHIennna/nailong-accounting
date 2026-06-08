package com.nailong.accounting.domain.usecase

import com.nailong.accounting.domain.repository.BootstrapRepository

class InitializeDefaultDataUseCase(
    private val bootstrapRepository: BootstrapRepository,
) {
    suspend operator fun invoke() {
        bootstrapRepository.initializeDefaultsIfNeeded()
    }
}
