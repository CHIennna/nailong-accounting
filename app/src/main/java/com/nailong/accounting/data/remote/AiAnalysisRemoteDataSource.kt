package com.nailong.accounting.data.remote

import com.nailong.accounting.domain.repository.AiAnalysisInput
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiAnalysisRemoteDataSource(
    initialBaseUrl: String,
) {
    @Volatile
    private var baseUrl: String = initialBaseUrl

    fun updateBaseUrl(value: String) {
        baseUrl = value
    }

    suspend fun generateReport(input: AiAnalysisInput): JSONObject =
        withContext(Dispatchers.IO) {
            val url = URL("${baseUrl.trimEnd('/')}/ai/expense-analysis")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val body = input.toJson()
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            connection.disconnect()

            if (code !in 200..299) {
                throw IllegalStateException(parseErrorMessage(text))
            }
            JSONObject(text)
        }

    private fun AiAnalysisInput.toJson(): JSONObject {
        val categories = JSONArray()
        categoryExpenses.forEach { item ->
            categories.put(
                JSONObject()
                    .put("categoryName", item.categoryName)
                    .put("amountInCents", item.amountInCents)
                    .put("percentage", item.percentage)
                    .put("monthOverMonthChangeRate", JSONObject.NULL),
            )
        }

        val summary = JSONObject()
            .put("incomeInCents", incomeInCents)
            .put("expenseInCents", expenseInCents)
            .put("balanceInCents", balanceInCents)
            .put("budgetInCents", budgetInCents ?: JSONObject.NULL)
            .put("budgetUsageRate", budgetUsageRate ?: JSONObject.NULL)
            .put("transactionCount", transactionCount)
            .put("categoryExpenses", categories)
            .put("dailyAverageExpenseInCents", dailyAverageExpenseInCents ?: JSONObject.NULL)
            .put("topExpenseCategoryName", topExpenseCategoryName ?: JSONObject.NULL)

        return JSONObject()
            .put("requestId", requestId)
            .put("ledgerName", ledgerName)
            .put("period", period)
            .put("currency", "CNY")
            .put("summary", summary)
    }

    private fun parseErrorMessage(text: String): String =
        runCatching {
            val detail = JSONObject(text).optJSONObject("detail")
            detail?.optString("message")?.takeIf { it.isNotBlank() }
                ?: JSONObject(text).optString("message")
        }.getOrDefault("AI 分析生成失败")
}
