from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class CategoryExpense(BaseModel):
    category_name: str = Field(alias="categoryName", min_length=1)
    amount_in_cents: int = Field(alias="amountInCents", ge=0)
    percentage: float = Field(ge=0)
    month_over_month_change_rate: float | None = Field(
        default=None,
        alias="monthOverMonthChangeRate",
    )


class ExpenseSummary(BaseModel):
    income_in_cents: int = Field(alias="incomeInCents", ge=0)
    expense_in_cents: int = Field(alias="expenseInCents", ge=0)
    balance_in_cents: int = Field(alias="balanceInCents")
    budget_in_cents: int | None = Field(default=None, alias="budgetInCents", ge=0)
    budget_usage_rate: float | None = Field(default=None, alias="budgetUsageRate", ge=0)
    transaction_count: int = Field(alias="transactionCount", ge=0)
    category_expenses: list[CategoryExpense] = Field(alias="categoryExpenses", default_factory=list)
    daily_average_expense_in_cents: int | None = Field(
        default=None,
        alias="dailyAverageExpenseInCents",
        ge=0,
    )
    top_expense_category_name: str | None = Field(default=None, alias="topExpenseCategoryName")


class ExpenseAnalysisRequest(BaseModel):
    request_id: str = Field(alias="requestId", min_length=1)
    ledger_name: str = Field(alias="ledgerName", min_length=1)
    period: str = Field(pattern=r"^\d{4}-\d{2}$")
    currency: Literal["CNY"] = "CNY"
    summary: ExpenseSummary


class ExpenseAnalysisReport(BaseModel):
    summary: str = Field(min_length=1, max_length=200)
    main_categories: list[str] = Field(alias="mainCategories", max_length=5)
    alerts: list[str] = Field(max_length=5)
    budget_comment: str = Field(alias="budgetComment", min_length=1, max_length=200)
    suggestions: list[str] = Field(max_length=5)
    encouragement: str = Field(min_length=1, max_length=100)


class ExpenseAnalysisResponse(BaseModel):
    request_id: str = Field(alias="requestId")
    report: ExpenseAnalysisReport
    model: str
    generated_at: datetime = Field(alias="generatedAt")
