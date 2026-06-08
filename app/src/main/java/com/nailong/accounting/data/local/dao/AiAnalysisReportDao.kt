package com.nailong.accounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nailong.accounting.data.local.entity.AiAnalysisReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiAnalysisReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AiAnalysisReportEntity)

    @Query(
        "SELECT * FROM ai_analysis_reports " +
            "WHERE ledgerId = :ledgerId AND period = :period AND status = 'generated' AND deletedAt IS NULL " +
            "ORDER BY generatedAt DESC LIMIT 1",
    )
    suspend fun getLatestGenerated(ledgerId: String, period: String): AiAnalysisReportEntity?

    @Query(
        "SELECT * FROM ai_analysis_reports " +
            "WHERE ledgerId = :ledgerId AND period = :period AND status = 'generated' AND deletedAt IS NULL " +
            "ORDER BY generatedAt DESC LIMIT 1",
    )
    fun observeLatestGenerated(ledgerId: String, period: String): Flow<AiAnalysisReportEntity?>

    @Query("UPDATE ai_analysis_reports SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :reportId")
    suspend fun softDelete(reportId: String, deletedAt: Long)
}
