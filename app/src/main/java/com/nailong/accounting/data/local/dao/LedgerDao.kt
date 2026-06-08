package com.nailong.accounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nailong.accounting.data.local.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LedgerEntity>)

    @Update
    suspend fun update(entity: LedgerEntity)

    @Query("SELECT * FROM ledgers WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): LedgerEntity?

    @Query("SELECT * FROM ledgers WHERE isDefault = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun getDefault(): LedgerEntity?

    @Query("SELECT * FROM ledgers WHERE isArchived = 0 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveLedgers(): Flow<List<LedgerEntity>>

    @Query("SELECT COUNT(*) FROM ledgers WHERE deletedAt IS NULL")
    suspend fun countActiveAndArchived(): Int

    @Query("UPDATE ledgers SET isDefault = 0, updatedAt = :updatedAt WHERE deletedAt IS NULL")
    suspend fun clearDefault(updatedAt: Long)

    @Query("UPDATE ledgers SET isDefault = 1, updatedAt = :updatedAt WHERE id = :ledgerId")
    suspend fun markDefault(ledgerId: String, updatedAt: Long)

    @Query("UPDATE ledgers SET isArchived = 1, updatedAt = :updatedAt WHERE id = :ledgerId")
    suspend fun archive(ledgerId: String, updatedAt: Long)

    @Query("UPDATE ledgers SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :ledgerId")
    suspend fun softDelete(ledgerId: String, deletedAt: Long)
}
