package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.CategoryDao
import com.nailong.accounting.data.local.entity.CategoryEntity
import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.TransactionType
import com.nailong.accounting.domain.repository.CategoryRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultCategoryRepository(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeCategories(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeByType(type.value).map { categories -> categories.map { it.toDomain() } }

    override suspend fun createCategory(name: String, type: TransactionType): Category {
        require(type != TransactionType.Transfer) { "转账类型不需要分类" }
        val now = System.currentTimeMillis()
        val entity = CategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type.value,
            icon = if (type == TransactionType.Expense) "tag" else "income",
            color = if (type == TransactionType.Expense) "#E8590C" else "#2F9E44",
            sortOrder = categoryDao.countActive() + 1,
            isSystem = false,
            isHidden = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        categoryDao.insert(entity)
        return entity.toDomain()
    }
}
