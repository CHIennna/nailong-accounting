package com.nailong.accounting.domain.model

data class AiAnalysisReport(
    val summary: String,
    val mainCategories: List<String>,
    val alerts: List<String>,
    val budgetComment: String,
    val suggestions: List<String>,
    val encouragement: String,
    val model: String,
    val generatedAt: Long,
)

enum class AiReportStatus {
    NotGenerated,
    Generating,
    Generated,
    Failed,
}
