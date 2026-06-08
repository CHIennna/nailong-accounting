package com.nailong.accounting.domain.repository

interface BootstrapRepository {
    suspend fun initializeDefaultsIfNeeded()
}
