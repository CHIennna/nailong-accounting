package com.nailong.accounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nailong.accounting.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: BudgetEntity)

    @Query(
        "SELECT * FROM budgets " +
            "WHERE ledgerId = :ledgerId AND period = :period AND categoryId IS NULL AND deletedAt IS NULL " +
            "LIMIT 1",
    )
    suspend fun getTotalBudget(ledgerId: String, period: String): BudgetEntity?

    @Query(
        "SELECT * FROM budgets " +
            "WHERE ledgerId = :ledgerId AND period = :period AND deletedAt IS NULL " +
            "ORDER BY categoryId IS NOT NULL, createdAt ASC",
    )
    fun observeByMonth(ledgerId: String, period: String): Flow<List<BudgetEntity>>

    @Query(
        "SELECT * FROM budgets " +
            "WHERE ledgerId = :ledgerId AND period = :period AND categoryId = :categoryId AND deletedAt IS NULL " +
            "LIMIT 1",
    )
    suspend fun getCategoryBudget(ledgerId: String, period: String, categoryId: String): BudgetEntity?

    @Query("UPDATE budgets SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :budgetId")
    suspend fun softDelete(budgetId: String, deletedAt: Long)
}
