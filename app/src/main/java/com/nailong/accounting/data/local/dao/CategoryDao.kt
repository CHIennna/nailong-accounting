package com.nailong.accounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nailong.accounting.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Update
    suspend fun update(entity: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): CategoryEntity?

    @Query(
        "SELECT * FROM categories " +
            "WHERE type = :type AND isHidden = 0 AND deletedAt IS NULL " +
            "ORDER BY sortOrder ASC",
    )
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("UPDATE categories SET isHidden = 1, updatedAt = :updatedAt WHERE id = :categoryId")
    suspend fun hide(categoryId: String, updatedAt: Long)

    @Query("UPDATE categories SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :categoryId")
    suspend fun softDelete(categoryId: String, deletedAt: Long)
}
