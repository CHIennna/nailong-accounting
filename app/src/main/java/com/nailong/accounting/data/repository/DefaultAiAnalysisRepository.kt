package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.AiAnalysisReportDao
import com.nailong.accounting.data.local.entity.AiAnalysisReportEntity
import com.nailong.accounting.data.remote.AiAnalysisRemoteDataSource
import com.nailong.accounting.domain.model.AiAnalysisReport
import com.nailong.accounting.domain.repository.AiAnalysisInput
import com.nailong.accounting.domain.repository.AiAnalysisRepository
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class DefaultAiAnalysisRepository(
    private val reportDao: AiAnalysisReportDao,
    private val remoteDataSource: AiAnalysisRemoteDataSource,
) : AiAnalysisRepository {
    override suspend fun getCachedReport(ledgerId: String, period: String): AiAnalysisReport? =
        reportDao.getLatestGenerated(ledgerId, period)?.toDomain()

    override fun updateBaseUrl(baseUrl: String) {
        remoteDataSource.updateBaseUrl(baseUrl)
    }

    override suspend fun generateReport(input: AiAnalysisInput): AiAnalysisReport {
        val response = remoteDataSource.generateReport(input)
        val reportJson = response.getJSONObject("report")
        val report = reportJson.toReport(
            model = response.optString("model", "deepseek"),
            generatedAt = System.currentTimeMillis(),
        )
        val now = System.currentTimeMillis()
        reportDao.insert(
            AiAnalysisReportEntity(
                id = UUID.randomUUID().toString(),
                ledgerId = input.ledgerId,
                period = input.period,
                summaryJson = input.toSummaryJson().toString(),
                reportJson = reportJson.toString(),
                modelName = report.model,
                status = "generated",
                generatedAt = now,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        return report
    }

    private fun AiAnalysisReportEntity.toDomain(): AiAnalysisReport =
        JSONObject(reportJson).toReport(model = modelName, generatedAt = generatedAt)

    private fun JSONObject.toReport(
        model: String,
        generatedAt: Long,
    ): AiAnalysisReport =
        AiAnalysisReport(
            summary = getString("summary"),
            mainCategories = getStringList("mainCategories"),
            alerts = getStringList("alerts"),
            budgetComment = getString("budgetComment"),
            suggestions = getStringList("suggestions"),
            encouragement = getString("encouragement"),
            model = model,
            generatedAt = generatedAt,
        )

    private fun JSONObject.getStringList(key: String): List<String> {
        val array = optJSONArray(key) ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                add(array.optString(index))
            }
        }.filter { it.isNotBlank() }
    }

    private fun AiAnalysisInput.toSummaryJson(): JSONObject =
        JSONObject()
            .put("requestId", requestId)
            .put("ledgerId", ledgerId)
            .put("period", period)
            .put("incomeInCents", incomeInCents)
            .put("expenseInCents", expenseInCents)
            .put("balanceInCents", balanceInCents)
            .put("budgetInCents", budgetInCents ?: JSONObject.NULL)
            .put("budgetUsageRate", budgetUsageRate ?: JSONObject.NULL)
            .put("transactionCount", transactionCount)
}
