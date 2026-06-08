from datetime import datetime, timezone
from typing import Any

from fastapi import HTTPException, status
from pydantic import ValidationError

from app.core.config import settings
from app.schemas.ai import ExpenseAnalysisReport, ExpenseAnalysisRequest, ExpenseAnalysisResponse
from app.services.deepseek_client import DeepSeekClient


class ExpenseAnalysisService:
    def __init__(self, client: DeepSeekClient | None = None) -> None:
        self._client = client or DeepSeekClient()

    async def generate(self, request: ExpenseAnalysisRequest) -> ExpenseAnalysisResponse:
        messages = [
            {"role": "system", "content": self._system_prompt()},
            {"role": "user", "content": self._user_prompt(request)},
        ]

        try:
            report_payload = await self._client.create_json_chat_completion(messages)
            report = ExpenseAnalysisReport.model_validate(report_payload)
        except (ValidationError, KeyError, ValueError) as exc:
            raise self._provider_error(request.request_id, "AI 返回格式不符合要求。") from exc
        except Exception as exc:
            raise self._provider_error(request.request_id, "AI 服务暂时不可用，请稍后重试。") from exc

        return ExpenseAnalysisResponse(
            requestId=request.request_id,
            report=report,
            model=settings.deepseek_model,
            generatedAt=datetime.now(timezone.utc),
        )

    def _system_prompt(self) -> str:
        return (
            "你是“奶龙记账”的消费分析助手。你需要根据用户提供的月度收支统计摘要，"
            "生成简洁、温和、实用的中文消费分析报告。"
            "要求：1. 只能基于输入摘要分析，不要编造具体账单。"
            "2. 不输出医疗、法律、投资等专业建议。"
            "3. 语气友好，不批评用户。"
            "4. 建议必须具体、可执行。"
            "5. 输出必须是 JSON 对象，不要输出 Markdown。"
            "6. JSON 字段必须包含 summary、mainCategories、alerts、budgetComment、suggestions、encouragement。"
        )

    def _user_prompt(self, request: ExpenseAnalysisRequest) -> str:
        summary = request.summary
        categories = [
            {
                "categoryName": item.category_name,
                "amountInCents": item.amount_in_cents,
                "percentage": item.percentage,
                "monthOverMonthChangeRate": item.month_over_month_change_rate,
            }
            for item in summary.category_expenses
        ]

        return (
            "请根据以下脱敏月度消费摘要生成消费分析报告。\n"
            f"账本：{request.ledger_name}\n"
            f"月份：{request.period}\n"
            f"币种：{request.currency}\n"
            f"收入：{summary.income_in_cents}\n"
            f"支出：{summary.expense_in_cents}\n"
            f"结余：{summary.balance_in_cents}\n"
            f"预算：{summary.budget_in_cents}\n"
            f"预算使用率：{summary.budget_usage_rate}\n"
            f"账单数量：{summary.transaction_count}\n"
            f"分类支出：{categories}\n"
            f"日均支出：{summary.daily_average_expense_in_cents}\n"
            f"最高支出分类：{summary.top_expense_category_name}\n"
            "请输出 JSON："
            '{"summary":"string","mainCategories":["string"],"alerts":["string"],'
            '"budgetComment":"string","suggestions":["string"],"encouragement":"string"}'
        )

    def _provider_error(self, request_id: str, message: str) -> HTTPException:
        return HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={
                "code": "AI_502_PROVIDER_FAILED",
                "message": message,
                "requestId": request_id,
            },
        )
