package com.nailong.accounting.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_analysis_reports",
    indices = [
        Index(value = ["ledgerId", "period", "generatedAt"]),
    ],
)
data class AiAnalysisReportEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val period: String,
    val summaryJson: String,
    val reportJson: String,
    val modelName: String,
    val status: String,
    val generatedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
