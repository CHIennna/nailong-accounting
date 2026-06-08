package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.CategoryDao
import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.TransactionType
import com.nailong.accounting.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultCategoryRepository(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeCategories(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeByType(type.value).map { categories -> categories.map { it.toDomain() } }
}
