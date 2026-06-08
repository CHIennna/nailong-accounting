package com.nailong.accounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nailong.accounting.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): TransactionEntity?

    @Query(
        "SELECT * FROM transactions " +
            "WHERE ledgerId = :ledgerId AND deletedAt IS NULL " +
            "ORDER BY date DESC, createdAt DESC LIMIT :limit",
    )
    fun observeRecent(ledgerId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE ledgerId = :ledgerId AND date BETWEEN :startAt AND :endAt AND deletedAt IS NULL " +
            "ORDER BY date DESC, createdAt DESC",
    )
    fun observeByDateRange(
        ledgerId: String,
        startAt: Long,
        endAt: Long,
    ): Flow<List<TransactionEntity>>

    @Query(
        "SELECT COUNT(*) FROM transactions " +
            "WHERE ledgerId = :ledgerId AND date BETWEEN :startAt AND :endAt AND deletedAt IS NULL",
    )
    suspend fun countByDateRange(ledgerId: String, startAt: Long, endAt: Long): Int

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE ledgerId = :ledgerId AND type = 'expense' " +
            "AND date BETWEEN :startAt AND :endAt AND deletedAt IS NULL",
    )
    suspend fun sumExpenseByDateRange(ledgerId: String, startAt: Long, endAt: Long): Long

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE ledgerId = :ledgerId AND type = 'income' " +
            "AND date BETWEEN :startAt AND :endAt AND deletedAt IS NULL",
    )
    suspend fun sumIncomeByDateRange(ledgerId: String, startAt: Long, endAt: Long): Long

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE ledgerId = :ledgerId AND categoryId = :categoryId AND type = 'expense' " +
            "AND date BETWEEN :startAt AND :endAt AND deletedAt IS NULL",
    )
    suspend fun sumExpenseByCategory(
        ledgerId: String,
        categoryId: String,
        startAt: Long,
        endAt: Long,
    ): Long

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :transactionId")
    suspend fun softDelete(transactionId: String, deletedAt: Long)
}
