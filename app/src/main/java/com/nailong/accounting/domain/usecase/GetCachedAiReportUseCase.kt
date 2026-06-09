package com.nailong.accounting.domain.usecase

import com.nailong.accounting.domain.model.AiAnalysisReport
import com.nailong.accounting.domain.repository.AiAnalysisRepository

class GetCachedAiReportUseCase(
    private val aiAnalysisRepository: AiAnalysisRepository,
) {
    suspend operator fun invoke(ledgerId: String, period: String): AiAnalysisReport? =
        aiAnalysisRepository.getCachedReport(ledgerId, period)
}
