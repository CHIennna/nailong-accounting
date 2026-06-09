package com.nailong.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nailong.accounting.domain.model.AiAnalysisReport
import com.nailong.accounting.domain.model.AiReportStatus
import com.nailong.accounting.domain.model.Account
import com.nailong.accounting.domain.model.BudgetStatus
import com.nailong.accounting.domain.model.BudgetUsage
import com.nailong.accounting.domain.model.CategoryBudgetUsage
import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.CategoryExpenseAnalysis
import com.nailong.accounting.domain.model.DailyExpensePoint
import com.nailong.accounting.domain.model.Ledger
import com.nailong.accounting.domain.model.MonthlySummary
import com.nailong.accounting.domain.model.Transaction
import com.nailong.accounting.domain.model.TransactionType
import com.nailong.accounting.domain.repository.AccountRepository
import com.nailong.accounting.domain.repository.AiAnalysisInput
import com.nailong.accounting.domain.repository.AiAnalysisRepository
import com.nailong.accounting.domain.repository.AiCategoryExpenseInput
import com.nailong.accounting.domain.repository.AppSettingRepository
import com.nailong.accounting.domain.repository.BudgetRepository
import com.nailong.accounting.domain.repository.CategoryRepository
import com.nailong.accounting.domain.repository.LedgerRepository
import com.nailong.accounting.domain.repository.TransactionInput
import com.nailong.accounting.domain.repository.TransactionRepository
import com.nailong.accounting.domain.usecase.AddTransactionUseCase
import com.nailong.accounting.domain.usecase.DeleteTransactionUseCase
import com.nailong.accounting.domain.usecase.GenerateAiReportUseCase
import com.nailong.accounting.domain.usecase.GetCachedAiReportUseCase
import com.nailong.accounting.domain.usecase.InitializeDefaultDataUseCase
import com.nailong.accounting.domain.usecase.SetCategoryBudgetUseCase
import com.nailong.accounting.domain.usecase.SetMonthlyBudgetUseCase
import com.nailong.accounting.domain.usecase.UpdateTransactionUseCase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
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

private const val KEY_AI_BASE_URL = "ai_base_url"
private const val DEFAULT_API_BASE_URL = "http://10.0.2.2:8000/api/v1"

data class AccountingUiState(
    val isLoading: Boolean = true,
    val ledgers: List<Ledger> = emptyList(),
    val currentLedger: Ledger? = null,
    val ledgerNameText: String = "",
    val accountNameText: String = "",
    val categoryNameText: String = "",
    val managedCategoryType: TransactionType = TransactionType.Expense,
    val managedCategories: List<Category> = emptyList(),
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
    val expenseCategories: List<Category> = emptyList(),
    val selectedBudgetCategoryId: String? = null,
    val categoryBudgetAmountText: String = "",
    val categoryBudgetUsages: List<CategoryBudgetUsage> = emptyList(),
    val categoryExpenseAnalysis: List<CategoryExpenseAnalysis> = emptyList(),
    val dailyExpenseTrend: List<DailyExpensePoint> = emptyList(),
    val aiReportStatus: AiReportStatus = AiReportStatus.NotGenerated,
    val aiReport: AiAnalysisReport? = null,
    val aiErrorMessage: String? = null,
    val aiBaseUrl: String = DEFAULT_API_BASE_URL,
    val aiBaseUrlText: String = DEFAULT_API_BASE_URL,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val monthlyTransactions: List<Transaction> = emptyList(),
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
    private val appSettingRepository: AppSettingRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val initializeDefaultDataUseCase: InitializeDefaultDataUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val setCategoryBudgetUseCase: SetCategoryBudgetUseCase,
    private val getCachedAiReportUseCase: GetCachedAiReportUseCase,
    private val generateAiReportUseCase: GenerateAiReportUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountingUiState())
    val uiState: StateFlow<AccountingUiState> = _uiState.asStateFlow()

    private var recentJob: Job? = null
    private var categoryJob: Job? = null
    private var monthlyJob: Job? = null
    private var budgetJob: Job? = null
    private var expenseCategoryJob: Job? = null
    private var managedCategoryJob: Job? = null
    private var appSettingJob: Job? = null

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

    fun updateLedgerName(value: String) {
        _uiState.update { it.copy(ledgerNameText = value, message = null) }
    }

    fun updateAccountName(value: String) {
        _uiState.update { it.copy(accountNameText = value, message = null) }
    }

    fun updateCategoryName(value: String) {
        _uiState.update { it.copy(categoryNameText = value, message = null) }
    }

    fun selectManagedCategoryType(type: TransactionType) {
        if (type == TransactionType.Transfer || _uiState.value.managedCategoryType == type) return
        _uiState.update {
            it.copy(
                managedCategoryType = type,
                managedCategories = emptyList(),
                categoryNameText = "",
                message = null,
            )
        }
        observeManagedCategories(type)
    }

    fun updateAiBaseUrl(value: String) {
        _uiState.update { it.copy(aiBaseUrlText = value, message = null) }
    }

    fun saveAiBaseUrl() {
        viewModelScope.launch {
            val normalized = normalizeBaseUrl(_uiState.value.aiBaseUrlText)
            if (normalized == null) {
                showMessage("请输入以 http:// 或 https:// 开头的后端地址")
                return@launch
            }

            runCatching { appSettingRepository.saveString(KEY_AI_BASE_URL, normalized) }
                .onSuccess { showMessage("AI 后端地址已保存") }
                .onFailure { showMessage(it.message ?: "AI 后端地址保存失败") }
        }
    }

    fun resetAiBaseUrl() {
        viewModelScope.launch {
            runCatching { appSettingRepository.saveString(KEY_AI_BASE_URL, DEFAULT_API_BASE_URL) }
                .onSuccess { showMessage("AI 后端地址已恢复默认") }
                .onFailure { showMessage(it.message ?: "AI 后端地址恢复失败") }
        }
    }

    fun createLedger() {
        viewModelScope.launch {
            val name = _uiState.value.ledgerNameText.trim()
            if (name.isBlank()) {
                showMessage("请输入账本名称")
                return@launch
            }
            runCatching { ledgerRepository.createLedger(name) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            ledgerNameText = "",
                            message = "账本已创建",
                        )
                    }
                }
                .onFailure { showMessage(it.message ?: "账本创建失败") }
        }
    }

    fun selectLedger(ledgerId: String) {
        if (_uiState.value.currentLedger?.id == ledgerId) return
        viewModelScope.launch {
            runCatching { ledgerRepository.setDefaultLedger(ledgerId) }
                .onSuccess { showMessage("已切换账本") }
                .onFailure { showMessage(it.message ?: "账本切换失败") }
        }
    }

    fun createAccount() {
        viewModelScope.launch {
            val name = _uiState.value.accountNameText.trim()
            if (name.isBlank()) {
                showMessage("请输入账户名称")
                return@launch
            }
            runCatching { accountRepository.createAccount(name) }
                .onSuccess { account ->
                    _uiState.update {
                        it.copy(
                            accountNameText = "",
                            selectedAccountId = account.id,
                            message = "账户已创建",
                        )
                    }
                }
                .onFailure { showMessage(it.message ?: "账户创建失败") }
        }
    }

    fun createCategory() {
        viewModelScope.launch {
            val state = _uiState.value
            val name = state.categoryNameText.trim()
            if (name.isBlank()) {
                showMessage("请输入分类名称")
                return@launch
            }
            runCatching { categoryRepository.createCategory(name, state.managedCategoryType) }
                .onSuccess { category ->
                    _uiState.update {
                        it.copy(
                            categoryNameText = "",
                            selectedCategoryId = if (it.selectedType == category.type) category.id else it.selectedCategoryId,
                            selectedBudgetCategoryId = if (category.type == TransactionType.Expense) category.id else it.selectedBudgetCategoryId,
                            message = "分类已创建",
                        )
                    }
                }
                .onFailure { showMessage(it.message ?: "分类创建失败") }
        }
    }

    fun updateBudgetAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(budgetAmountText = sanitized, message = null) }
    }

    fun updateCategoryBudgetAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(categoryBudgetAmountText = sanitized, message = null) }
    }

    fun selectBudgetCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedBudgetCategoryId = categoryId, message = null) }
    }

    fun movePeriod(monthDelta: Int) {
        val currentLedger = _uiState.value.currentLedger
        _uiState.update {
            it.copy(
                currentPeriod = shiftPeriod(it.currentPeriod, monthDelta),
                monthlySummary = null,
                budgetUsage = emptyBudgetUsage(),
                categoryBudgetUsages = emptyList(),
                categoryExpenseAnalysis = emptyList(),
                dailyExpenseTrend = emptyList(),
                aiReportStatus = AiReportStatus.NotGenerated,
                aiReport = null,
                aiErrorMessage = null,
                message = null,
            )
        }
        currentLedger?.let { observeMonthlyTransactions(it.id) }
    }

    fun goToCurrentPeriod() {
        val currentLedger = _uiState.value.currentLedger
        _uiState.update {
            it.copy(
                currentPeriod = defaultCurrentPeriodText(),
                monthlySummary = null,
                budgetUsage = emptyBudgetUsage(),
                categoryBudgetUsages = emptyList(),
                categoryExpenseAnalysis = emptyList(),
                dailyExpenseTrend = emptyList(),
                aiReportStatus = AiReportStatus.NotGenerated,
                aiReport = null,
                aiErrorMessage = null,
                message = null,
            )
        }
        currentLedger?.let { observeMonthlyTransactions(it.id) }
    }

    fun generateAiReport(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val ledger = state.currentLedger
            val summary = state.monthlySummary
            if (ledger == null || summary == null) {
                showMessage("月度统计未就绪")
                return@launch
            }

            _uiState.update {
                it.copy(
                    aiReportStatus = AiReportStatus.Generating,
                    aiErrorMessage = null,
                )
            }

            runCatching {
                if (!forceRefresh) {
                    getCachedAiReportUseCase(ledger.id, state.currentPeriod)?.let { return@runCatching it }
                }
                generateAiReportUseCase(buildAiInput(state, ledger.name, ledger.id, summary))
            }
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            aiReport = report,
                            aiReportStatus = AiReportStatus.Generated,
                            aiErrorMessage = null,
                            message = "AI 月报已生成",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            aiReportStatus = AiReportStatus.Failed,
                            aiErrorMessage = error.message ?: "AI 月报生成失败",
                        )
                    }
                }
        }
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

    fun saveCategoryBudget() {
        viewModelScope.launch {
            val state = _uiState.value
            val ledger = state.currentLedger
            val categoryId = state.selectedBudgetCategoryId
            if (ledger == null) {
                showMessage("账本未就绪")
                return@launch
            }
            if (categoryId == null) {
                showMessage("请选择分类")
                return@launch
            }
            val amount = parseAmountInCents(state.categoryBudgetAmountText)
            if (amount == null) {
                showMessage("请输入有效分类预算金额")
                return@launch
            }

            runCatching { setCategoryBudgetUseCase(ledger.id, state.currentPeriod, categoryId, amount) }
                .onSuccess {
                    _uiState.update { it.copy(categoryBudgetAmountText = "", message = "分类预算已保存") }
                }
                .onFailure { showMessage(it.message ?: "分类预算保存失败") }
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
            observeAppSettings()
            observeLedgers()
            observeAccounts()
            observeCategories(TransactionType.Expense)
            observeExpenseCategories()
            observeManagedCategories(TransactionType.Expense)
        }
    }

    private fun observeAppSettings() {
        appSettingJob?.cancel()
        appSettingJob = viewModelScope.launch {
            appSettingRepository.observeString(KEY_AI_BASE_URL, DEFAULT_API_BASE_URL)
                .catch { showMessage("设置加载失败") }
                .collect { baseUrl ->
                    aiAnalysisRepository.updateBaseUrl(baseUrl)
                    _uiState.update {
                        it.copy(
                            aiBaseUrl = baseUrl,
                            aiBaseUrlText = baseUrl,
                        )
                    }
                }
        }
    }

    private fun observeLedgers() {
        viewModelScope.launch {
            ledgerRepository.observeLedgers()
                .catch { showMessage("账本加载失败") }
                .collect { ledgers ->
                    val current = ledgers.firstOrNull { it.isDefault } ?: ledgers.firstOrNull()
                    val previousLedgerId = _uiState.value.currentLedger?.id
                    val ledgerChanged = previousLedgerId != null && current?.id != previousLedgerId
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            ledgers = ledgers,
                            currentLedger = current,
                            monthlySummary = if (ledgerChanged) null else it.monthlySummary,
                            budgetUsage = if (ledgerChanged) emptyBudgetUsage() else it.budgetUsage,
                            categoryBudgetUsages = if (ledgerChanged) emptyList() else it.categoryBudgetUsages,
                            categoryExpenseAnalysis = if (ledgerChanged) emptyList() else it.categoryExpenseAnalysis,
                            dailyExpenseTrend = if (ledgerChanged) emptyList() else it.dailyExpenseTrend,
                            aiReportStatus = if (ledgerChanged) AiReportStatus.NotGenerated else it.aiReportStatus,
                            aiReport = if (ledgerChanged) null else it.aiReport,
                            aiErrorMessage = if (ledgerChanged) null else it.aiErrorMessage,
                        )
                    }
                    current?.let {
                        observeRecentTransactions(it.id)
                        observeMonthlyTransactions(it.id)
                        loadCachedAiReport(it.id, _uiState.value.currentPeriod)
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

    private fun observeExpenseCategories() {
        expenseCategoryJob?.cancel()
        expenseCategoryJob = viewModelScope.launch {
            categoryRepository.observeCategories(TransactionType.Expense)
                .catch { showMessage("预算分类加载失败") }
                .collect { categories ->
                    val selected = _uiState.value.selectedBudgetCategoryId ?: categories.firstOrNull()?.id
                    _uiState.update {
                        it.copy(
                            expenseCategories = categories,
                            selectedBudgetCategoryId = selected,
                        )
                    }
                    rebuildAnalysis()
                    val ledger = _uiState.value.currentLedger
                    if (ledger != null && _uiState.value.monthlySummary != null) {
                        observeBudgetUsages(
                            ledgerId = ledger.id,
                            period = _uiState.value.currentPeriod,
                            transactions = _uiState.value.monthlyTransactions,
                        )
                    }
                }
        }
    }

    private fun observeManagedCategories(type: TransactionType) {
        managedCategoryJob?.cancel()
        managedCategoryJob = viewModelScope.launch {
            categoryRepository.observeCategories(type)
                .catch { showMessage("分类管理加载失败") }
                .collect { categories ->
                    _uiState.update { it.copy(managedCategories = categories) }
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
                    val categoryAnalysis = buildCategoryExpenseAnalysis(transactions, _uiState.value.expenseCategories)
                    val dailyTrend = buildDailyExpenseTrend(transactions)
                    _uiState.update {
                        it.copy(
                            monthlySummary = summary,
                            monthlyTransactions = transactions,
                            categoryExpenseAnalysis = categoryAnalysis,
                            dailyExpenseTrend = dailyTrend,
                        )
                    }
                    observeBudgetUsages(ledgerId, period, transactions)
                    loadCachedAiReport(ledgerId, period)
                }
        }
    }

    private fun loadCachedAiReport(ledgerId: String, period: String) {
        viewModelScope.launch {
            runCatching { getCachedAiReportUseCase(ledgerId, period) }
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            aiReport = report,
                            aiReportStatus = if (report == null) AiReportStatus.NotGenerated else AiReportStatus.Generated,
                            aiErrorMessage = null,
                        )
                    }
                }
        }
    }

    private fun observeBudgetUsages(
        ledgerId: String,
        period: String,
        transactions: List<Transaction>,
    ) {
        budgetJob?.cancel()
        budgetJob = viewModelScope.launch {
            budgetRepository.observeBudgets(ledgerId, period)
                .catch { showMessage("预算加载失败") }
                .collect { budgets ->
                    val expenseTotal = transactions
                        .filter { it.type == TransactionType.Expense }
                        .sumOf { it.amountInCents }
                    val totalBudget = budgets.firstOrNull { it.categoryId == null }
                    val totalUsage = buildBudgetUsage(
                        budgetId = totalBudget?.id,
                        budgetAmount = totalBudget?.amountInCents,
                        usedAmount = expenseTotal,
                        alertThreshold = totalBudget?.alertThreshold ?: 80,
                    )
                    val categories = _uiState.value.expenseCategories
                    val categoryUsages = budgets
                        .filter { it.categoryId != null }
                        .mapNotNull { budget ->
                            val category = categories.firstOrNull { it.id == budget.categoryId } ?: return@mapNotNull null
                            val used = transactions
                                .filter {
                                    it.type == TransactionType.Expense &&
                                        it.categoryId == budget.categoryId
                                }
                                .sumOf { it.amountInCents }
                            val usage = buildBudgetUsage(
                                budgetId = budget.id,
                                budgetAmount = budget.amountInCents,
                                usedAmount = used,
                                alertThreshold = budget.alertThreshold,
                            )
                            CategoryBudgetUsage(
                                categoryId = category.id,
                                categoryName = category.name,
                                budgetAmountInCents = budget.amountInCents,
                                usedAmountInCents = used,
                                remainingAmountInCents = usage.remainingAmountInCents ?: 0,
                                usageRate = usage.usageRate ?: 0.0,
                                status = usage.status,
                            )
                        }
                    _uiState.update { state ->
                        state.copy(
                            budgetUsage = totalUsage,
                            categoryBudgetUsages = categoryUsages,
                            monthlySummary = state.monthlySummary?.copy(
                                budgetInCents = totalUsage.budgetAmountInCents,
                                budgetUsageRate = totalUsage.usageRate,
                            ),
                        )
                    }
                }
        }
    }

    private fun buildBudgetUsage(
        budgetId: String?,
        budgetAmount: Long?,
        usedAmount: Long,
        alertThreshold: Int,
    ): BudgetUsage {
        val remaining = budgetAmount?.minus(usedAmount)
        val rate = budgetAmount?.takeIf { it > 0 }?.let { usedAmount.toDouble() / it.toDouble() * 100 }
        val status = when {
            budgetAmount == null -> BudgetStatus.NotSet
            usedAmount >= budgetAmount -> BudgetStatus.Exceeded
            rate != null && rate >= alertThreshold -> BudgetStatus.Warning
            else -> BudgetStatus.Normal
        }
        return BudgetUsage(
            budgetId = budgetId,
            budgetAmountInCents = budgetAmount,
            usedAmountInCents = usedAmount,
            remainingAmountInCents = remaining,
            usageRate = rate,
            status = status,
        )
    }

    private fun rebuildAnalysis() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                categoryExpenseAnalysis = buildCategoryExpenseAnalysis(
                    transactions = state.monthlyTransactions,
                    categories = state.expenseCategories,
                ),
                dailyExpenseTrend = buildDailyExpenseTrend(state.monthlyTransactions),
            )
        }
    }

    private fun buildCategoryExpenseAnalysis(
        transactions: List<Transaction>,
        categories: List<Category>,
    ): List<CategoryExpenseAnalysis> {
        val expenseTransactions = transactions.filter { it.type == TransactionType.Expense }
        val total = expenseTransactions.sumOf { it.amountInCents }.takeIf { it > 0 } ?: return emptyList()
        return expenseTransactions
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, grouped) ->
                val category = categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
                val amount = grouped.sumOf { it.amountInCents }
                CategoryExpenseAnalysis(
                    categoryId = category.id,
                    categoryName = category.name,
                    amountInCents = amount,
                    percentage = amount.toDouble() / total.toDouble() * 100,
                )
            }
            .sortedByDescending { it.amountInCents }
    }

    private fun buildDailyExpenseTrend(transactions: List<Transaction>): List<DailyExpensePoint> {
        val dailyAmounts = transactions
            .filter { it.type == TransactionType.Expense }
            .groupBy { dayLabel(it.date) }
            .mapValues { (_, grouped) -> grouped.sumOf { it.amountInCents } }
            .toSortedMap()
        val maxAmount = dailyAmounts.values.maxOrNull()?.takeIf { it > 0 } ?: return emptyList()
        return dailyAmounts.map { (day, amount) ->
            DailyExpensePoint(
                dayLabel = day,
                amountInCents = amount,
                percentageOfMax = amount.toDouble() / maxAmount.toDouble() * 100,
            )
        }
    }

    private fun buildAiInput(
        state: AccountingUiState,
        ledgerName: String,
        ledgerId: String,
        summary: MonthlySummary,
    ): AiAnalysisInput {
        val dayCount = state.dailyExpenseTrend.size.takeIf { it > 0 }
        val dailyAverage = dayCount?.let { summary.expenseInCents / it }
        val topCategory = state.categoryExpenseAnalysis.firstOrNull()

        return AiAnalysisInput(
            requestId = UUID.randomUUID().toString(),
            ledgerId = ledgerId,
            ledgerName = ledgerName,
            period = state.currentPeriod,
            incomeInCents = summary.incomeInCents,
            expenseInCents = summary.expenseInCents,
            balanceInCents = summary.balanceInCents,
            budgetInCents = state.budgetUsage.budgetAmountInCents,
            budgetUsageRate = state.budgetUsage.usageRate,
            transactionCount = summary.transactionCount,
            categoryExpenses = state.categoryExpenseAnalysis.map {
                AiCategoryExpenseInput(
                    categoryName = it.categoryName,
                    amountInCents = it.amountInCents,
                    percentage = it.percentage,
                )
            },
            dailyAverageExpenseInCents = dailyAverage,
            topExpenseCategoryName = topCategory?.categoryName,
        )
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
        private val appSettingRepository: AppSettingRepository,
        private val aiAnalysisRepository: AiAnalysisRepository,
        private val initializeDefaultDataUseCase: InitializeDefaultDataUseCase,
        private val addTransactionUseCase: AddTransactionUseCase,
        private val updateTransactionUseCase: UpdateTransactionUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
        private val setCategoryBudgetUseCase: SetCategoryBudgetUseCase,
        private val getCachedAiReportUseCase: GetCachedAiReportUseCase,
        private val generateAiReportUseCase: GenerateAiReportUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AccountingViewModel(
                ledgerRepository = ledgerRepository,
                categoryRepository = categoryRepository,
                accountRepository = accountRepository,
                budgetRepository = budgetRepository,
                transactionRepository = transactionRepository,
                appSettingRepository = appSettingRepository,
                aiAnalysisRepository = aiAnalysisRepository,
                initializeDefaultDataUseCase = initializeDefaultDataUseCase,
                addTransactionUseCase = addTransactionUseCase,
                updateTransactionUseCase = updateTransactionUseCase,
                deleteTransactionUseCase = deleteTransactionUseCase,
                setMonthlyBudgetUseCase = setMonthlyBudgetUseCase,
                setCategoryBudgetUseCase = setCategoryBudgetUseCase,
                getCachedAiReportUseCase = getCachedAiReportUseCase,
                generateAiReportUseCase = generateAiReportUseCase,
            ) as T
    }

    companion object {
        private const val RECENT_LIMIT = 20

        private fun normalizeBaseUrl(value: String): String? {
            val trimmed = value.trim().trimEnd('/')
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
            return trimmed.takeIf { it.length > "http://".length }
        }

        private fun emptyBudgetUsage(): BudgetUsage =
            BudgetUsage(
                budgetId = null,
                budgetAmountInCents = null,
                usedAmountInCents = 0,
                remainingAmountInCents = null,
                usageRate = null,
                status = BudgetStatus.NotSet,
            )

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

        private fun shiftPeriod(period: String, monthDelta: Int): String {
            val parts = period.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val month = parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
            val calendar = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, monthDelta)
            }
            return SimpleDateFormat("yyyy-MM", Locale.CHINA).format(calendar.time)
        }

        private fun dayLabel(timestamp: Long): String =
            SimpleDateFormat("MM-dd", Locale.CHINA).format(timestamp)
    }
}
