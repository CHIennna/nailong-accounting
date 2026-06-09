package com.nailong.accounting.domain.usecase

import com.nailong.accounting.domain.model.AiAnalysisReport
import com.nailong.accounting.domain.repository.AiAnalysisInput
import com.nailong.accounting.domain.repository.AiAnalysisRepository

class GenerateAiReportUseCase(
    private val aiAnalysisRepository: AiAnalysisRepository,
) {
    suspend operator fun invoke(input: AiAnalysisInput): AiAnalysisReport {
        require(input.transactionCount >= 3) { "本月记录较少，至少 3 条账单后再生成 AI 分析" }
        return aiAnalysisRepository.generateReport(input)
    }
}
