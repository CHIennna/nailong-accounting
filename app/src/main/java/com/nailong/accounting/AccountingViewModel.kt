package com.nailong.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nailong.accounting.domain.model.Account
import com.nailong.accounting.domain.model.BudgetStatus
import com.nailong.accounting.domain.model.BudgetUsage
import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.Ledger
import com.nailong.accounting.domain.model.MonthlySummary
import com.nailong.accounting.domain.model.Transaction
import com.nailong.accounting.domain.model.TransactionType
import com.nailong.accounting.domain.repository.AccountRepository
import com.nailong.accounting.domain.repository.BudgetRepository
import com.nailong.accounting.domain.repository.CategoryRepository
import com.nailong.accounting.domain.repository.LedgerRepository
import com.nailong.accounting.domain.repository.TransactionInput
import com.nailong.accounting.domain.repository.TransactionRepository
import com.nailong.accounting.domain.usecase.AddTransactionUseCase
import com.nailong.accounting.domain.usecase.DeleteTransactionUseCase
import com.nailong.accounting.domain.usecase.InitializeDefaultDataUseCase
import com.nailong.accounting.domain.usecase.SetMonthlyBudgetUseCase
import com.nailong.accounting.domain.usecase.UpdateTransactionUseCase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private fun defaultCurrentPeriodText(): String =
    SimpleDateFormat("yyyy-MM", Locale.CHINA).format(Calendar.getInstance().time)

data class AccountingUiState(
    val isLoading: Boolean = true,
    val ledgers: List<Ledger> = emptyList(),
    val currentLedger: Ledger? = null,
    val currentPeriod: String = defaultCurrentPeriodText(),
    val monthlySummary: MonthlySummary? = null,
    val budgetUsage: BudgetUsage = BudgetUsage(
        budgetId = null,
        budgetAmountInCents = null,
        usedAmountInCents = 0,
        remainingAmountInCents = null,
        usageRate = null,
        status = BudgetStatus.NotSet,
    ),
    val budgetAmountText: String = "",
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val selectedType: TransactionType = TransactionType.Expense,
    val amountText: String = "",
    val noteText: String = "",
    val selectedCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val selectedTargetAccountId: String? = null,
    val editingTransactionId: String? = null,
    val message: String? = null,
)

class AccountingViewModel(
    private val ledgerRepository: LedgerRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val initializeDefaultDataUseCase: InitializeDefaultDataUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountingUiState())
    val uiState: StateFlow<AccountingUiState> = _uiState.asStateFlow()

    private var recentJob: Job? = null
    private var categoryJob: Job? = null
    private var monthlyJob: Job? = null
    private var budgetJob: Job? = null

    init {
        bootstrap()
    }

    fun selectType(type: TransactionType) {
        _uiState.update {
            it.copy(
                selectedType = type,
                selectedCategoryId = null,
                selectedTargetAccountId = null,
                message = null,
            )
        }
        observeCategories(type)
    }

    fun updateAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(amountText = sanitized, message = null) }
    }

    fun updateNote(value: String) {
        _uiState.update { it.copy(noteText = value, message = null) }
    }

    fun updateBudgetAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(budgetAmountText = sanitized, message = null) }
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, message = null) }
    }

    fun selectAccount(accountId: String?) {
        _uiState.update { it.copy(selectedAccountId = accountId, message = null) }
    }

    fun selectTargetAccount(accountId: String?) {
        _uiState.update { it.copy(selectedTargetAccountId = accountId, message = null) }
    }

    fun saveTransaction() {
        viewModelScope.launch {
            val state = _uiState.value
            val ledger = state.currentLedger
            if (ledger == null) {
                showMessage("账本未就绪")
                return@launch
            }

            val amount = parseAmountInCents(state.amountText)
            if (amount == null) {
                showMessage("请输入有效金额")
                return@launch
            }

            val accountId = state.selectedAccountId ?: state.accounts.firstOrNull()?.id
            if (accountId == null) {
                showMessage("请选择账户")
                return@launch
            }

            val input = TransactionInput(
                ledgerId = ledger.id,
                type = state.selectedType,
                amountInCents = amount,
                categoryId = if (state.selectedType == TransactionType.Transfer) null else state.selectedCategoryId,
                accountId = accountId,
                targetAccountId = state.selectedTargetAccountId,
                date = System.currentTimeMillis(),
                note = state.noteText,
            )

            runCatching {
                val editingId = state.editingTransactionId
                if (editingId == null) {
                    addTransactionUseCase(input)
                } else {
                    updateTransactionUseCase(editingId, input)
                    editingId
                }
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            amountText = "",
                            noteText = "",
                            selectedCategoryId = null,
                            selectedTargetAccountId = null,
                            editingTransactionId = null,
                            message = if (state.editingTransactionId == null) "已记一笔" else "已更新账单",
                        )
                    }
                }
                .onFailure { error -> showMessage(error.message ?: "保存失败") }
        }
    }

    fun saveMonthlyBudget() {
        viewModelScope.launch {
            val state = _uiState.value
            val ledger = state.currentLedger
            if (ledger == null) {
                showMessage("账本未就绪")
                return@launch
            }
            val amount = parseAmountInCents(state.budgetAmountText)
            if (amount == null) {
                showMessage("请输入有效预算金额")
                return@launch
            }

            runCatching { setMonthlyBudgetUseCase(ledger.id, state.currentPeriod, amount) }
                .onSuccess {
                    _uiState.update { it.copy(budgetAmountText = "", message = "预算已保存") }
                }
                .onFailure { showMessage(it.message ?: "预算保存失败") }
        }
    }

    fun editTransaction(transaction: Transaction) {
        _uiState.update {
            it.copy(
                selectedType = transaction.type,
                amountText = centsToAmountText(transaction.amountInCents),
                noteText = transaction.note,
                selectedCategoryId = transaction.categoryId,
                selectedAccountId = transaction.accountId,
                selectedTargetAccountId = transaction.targetAccountId,
                editingTransactionId = transaction.id,
                message = null,
            )
        }
        observeCategories(transaction.type)
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(
                amountText = "",
                noteText = "",
                selectedCategoryId = null,
                selectedTargetAccountId = null,
                editingTransactionId = null,
                message = null,
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            runCatching { deleteTransactionUseCase(transactionId) }
                .onSuccess {
                    if (_uiState.value.editingTransactionId == transactionId) {
                        cancelEdit()
                    }
                    showMessage("已删除账单")
                }
                .onFailure { showMessage(it.message ?: "删除失败") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            initializeDefaultDataUseCase()
            observeLedgers()
            observeAccounts()
            observeCategories(TransactionType.Expense)
        }
    }

    private fun observeLedgers() {
        viewModelScope.launch {
            ledgerRepository.observeLedgers()
                .catch { showMessage("账本加载失败") }
                .collect { ledgers ->
                    val current = ledgers.firstOrNull { it.isDefault } ?: ledgers.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            ledgers = ledgers,
                            currentLedger = current,
                        )
                    }
                    current?.let {
                        observeRecentTransactions(it.id)
                        observeMonthlyTransactions(it.id)
                    }
                }
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            accountRepository.observeAccounts()
                .catch { showMessage("账户加载失败") }
                .collect { accounts ->
                    val selected = _uiState.value.selectedAccountId ?: accounts.firstOrNull { it.isDefault }?.id
                    _uiState.update { it.copy(accounts = accounts, selectedAccountId = selected) }
                }
        }
    }

    private fun observeCategories(type: TransactionType) {
        categoryJob?.cancel()
        if (type == TransactionType.Transfer) {
            _uiState.update { it.copy(categories = emptyList(), selectedCategoryId = null) }
            return
        }
        categoryJob = viewModelScope.launch {
            categoryRepository.observeCategories(type)
                .catch { showMessage("分类加载失败") }
                .collect { categories ->
                    val selected = _uiState.value.selectedCategoryId ?: categories.firstOrNull()?.id
                    _uiState.update { it.copy(categories = categories, selectedCategoryId = selected) }
                }
        }
    }

    private fun observeRecentTransactions(ledgerId: String) {
        recentJob?.cancel()
        recentJob = viewModelScope.launch {
            transactionRepository.observeRecentTransactions(ledgerId, RECENT_LIMIT)
                .catch { showMessage("最近账单加载失败") }
                .collect { transactions ->
                    _uiState.update { it.copy(recentTransactions = transactions) }
                }
        }
    }

    private fun observeMonthlyTransactions(ledgerId: String) {
        monthlyJob?.cancel()
        val period = _uiState.value.currentPeriod
        val range = monthRange(period)
        monthlyJob = viewModelScope.launch {
            transactionRepository.observeTransactionsByDateRange(ledgerId, range.first, range.last)
                .catch { showMessage("月度统计加载失败") }
                .collect { transactions ->
                    val income = transactions
                        .filter { it.type == TransactionType.Income }
                        .sumOf { it.amountInCents }
                    val expense = transactions
                        .filter { it.type == TransactionType.Expense }
                        .sumOf { it.amountInCents }
                    val summary = MonthlySummary(
                        ledgerId = ledgerId,
                        period = period,
                        incomeInCents = income,
                        expenseInCents = expense,
                        balanceInCents = income - expense,
                        budgetInCents = _uiState.value.budgetUsage.budgetAmountInCents,
                        budgetUsageRate = _uiState.value.budgetUsage.usageRate,
                        transactionCount = transactions.size,
                    )
                    _uiState.update { it.copy(monthlySummary = summary) }
                    observeBudgetUsage(ledgerId, period, expense)
                }
        }
    }

    private fun observeBudgetUsage(
        ledgerId: String,
        period: String,
        usedAmountInCents: Long,
    ) {
        budgetJob?.cancel()
        budgetJob = viewModelScope.launch {
            budgetRepository.observeTotalBudgetUsage(ledgerId, period, usedAmountInCents)
                .catch { showMessage("预算加载失败") }
                .collect { usage ->
                    _uiState.update { state ->
                        state.copy(
                            budgetUsage = usage,
                            monthlySummary = state.monthlySummary?.copy(
                                budgetInCents = usage.budgetAmountInCents,
                                budgetUsageRate = usage.usageRate,
                            ),
                        )
                    }
                }
        }
    }

    private fun parseAmountInCents(text: String): Long? {
        val value = text.toDoubleOrNull() ?: return null
        val cents = (value * 100).roundToLong()
        return cents.takeIf { it > 0 }
    }

    private fun centsToAmountText(cents: Long): String =
        if (cents % 100 == 0L) {
            (cents / 100).toString()
        } else {
            "%.2f".format(cents / 100.0)
        }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    class Factory(
        private val ledgerRepository: LedgerRepository,
        private val categoryRepository: CategoryRepository,
        private val accountRepository: AccountRepository,
        private val budgetRepository: BudgetRepository,
        private val transactionRepository: TransactionRepository,
        private val initializeDefaultDataUseCase: InitializeDefaultDataUseCase,
        private val addTransactionUseCase: AddTransactionUseCase,
        private val updateTransactionUseCase: UpdateTransactionUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AccountingViewModel(
                ledgerRepository = ledgerRepository,
                categoryRepository = categoryRepository,
                accountRepository = accountRepository,
                budgetRepository = budgetRepository,
                transactionRepository = transactionRepository,
                initializeDefaultDataUseCase = initializeDefaultDataUseCase,
                addTransactionUseCase = addTransactionUseCase,
                updateTransactionUseCase = updateTransactionUseCase,
                deleteTransactionUseCase = deleteTransactionUseCase,
                setMonthlyBudgetUseCase = setMonthlyBudgetUseCase,
            ) as T
    }

    companion object {
        private const val RECENT_LIMIT = 20

        private fun monthRange(period: String): LongRange {
            val parts = period.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val month = parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
            val start = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val end = start.clone() as Calendar
            end.add(Calendar.MONTH, 1)
            end.add(Calendar.MILLISECOND, -1)
            return start.timeInMillis..end.timeInMillis
        }
    }
}
