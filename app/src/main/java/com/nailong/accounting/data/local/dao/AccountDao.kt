package com.nailong.accounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nailong.accounting.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AccountEntity>)

    @Update
    suspend fun update(entity: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun getDefault(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isArchived = 0 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts WHERE deletedAt IS NULL")
    suspend fun countActiveAndArchived(): Int

    @Query("UPDATE accounts SET isDefault = 0, updatedAt = :updatedAt WHERE deletedAt IS NULL")
    suspend fun clearDefault(updatedAt: Long)

    @Query("UPDATE accounts SET isDefault = 1, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun markDefault(accountId: String, updatedAt: Long)

    @Query("UPDATE accounts SET isArchived = 1, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun archive(accountId: String, updatedAt: Long)

    @Query("UPDATE accounts SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :accountId")
    suspend fun softDelete(accountId: String, deletedAt: Long)
}
