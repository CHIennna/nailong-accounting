package com.nailong.accounting.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppSettingRepository {
    fun observeString(key: String, defaultValue: String): Flow<String>
    suspend fun saveString(key: String, value: String)
}
