from fastapi import APIRouter, HTTPException, status

from app.schemas.ai import ExpenseAnalysisRequest, ExpenseAnalysisResponse
from app.services.expense_analysis_service import ExpenseAnalysisService


router = APIRouter()


@router.post("/expense-analysis", response_model=ExpenseAnalysisResponse)
async def generate_expense_analysis(
    request: ExpenseAnalysisRequest,
) -> ExpenseAnalysisResponse:
    if request.summary.transaction_count < 3:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={
                "code": "AI_400_NOT_ENOUGH_DATA",
                "message": "本月记录较少，暂不生成完整 AI 分析。",
                "requestId": request.request_id,
            },
        )

    service = ExpenseAnalysisService()
    return await service.generate(request)
