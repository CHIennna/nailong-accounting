package com.nailong.accounting.domain.repository

import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(type: TransactionType): Flow<List<Category>>
}
