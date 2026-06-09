package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.AppSettingDao
import com.nailong.accounting.data.local.entity.AppSettingEntity
import com.nailong.accounting.domain.repository.AppSettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultAppSettingRepository(
    private val appSettingDao: AppSettingDao,
) : AppSettingRepository {
    override fun observeString(key: String, defaultValue: String): Flow<String> =
        appSettingDao.observe(key).map { setting -> setting?.value ?: defaultValue }

    override suspend fun saveString(key: String, value: String) {
        appSettingDao.upsert(
            AppSettingEntity(
                key = key,
                value = value,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
