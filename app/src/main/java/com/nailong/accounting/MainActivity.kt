package com.nailong.accounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nailong.accounting.domain.model.Account
import com.nailong.accounting.domain.model.BudgetStatus
import com.nailong.accounting.domain.model.CategoryBudgetUsage
import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.CategoryExpenseAnalysis
import com.nailong.accounting.domain.model.DailyExpensePoint
import com.nailong.accounting.domain.model.Transaction
import com.nailong.accounting.domain.model.TransactionType
import com.nailong.accounting.ui.theme.NailongAccountingTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NailongAccountingTheme {
                val viewModel: AccountingViewModel = viewModel(factory = appContainer.accountingViewModelFactory)
                NailongApp(viewModel)
            }
        }
    }
}

@Composable
private fun NailongApp(viewModel: AccountingViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    HeaderCard(state)
                }
                item {
                    MonthlySummaryCard(
                        state = state,
                        onBudgetAmountChange = viewModel::updateBudgetAmount,
                        onSaveBudget = viewModel::saveMonthlyBudget,
                        onPreviousMonth = { viewModel.movePeriod(-1) },
                        onNextMonth = { viewModel.movePeriod(1) },
                        onCurrentMonth = viewModel::goToCurrentPeriod,
                        onCategoryBudgetAmountChange = viewModel::updateCategoryBudgetAmount,
                        onBudgetCategorySelected = viewModel::selectBudgetCategory,
                        onSaveCategoryBudget = viewModel::saveCategoryBudget,
                    )
                }
                item {
                    ExpenseAnalysisCard(state)
                }
                item {
                    TransactionForm(
                        state = state,
                        onTypeSelected = viewModel::selectType,
                        onAmountChange = viewModel::updateAmount,
                        onNoteChange = viewModel::updateNote,
                        onCategorySelected = viewModel::selectCategory,
                        onAccountSelected = viewModel::selectAccount,
                        onTargetAccountSelected = viewModel::selectTargetAccount,
                        onSave = viewModel::saveTransaction,
                        onCancelEdit = viewModel::cancelEdit,
                    )
                }
                item {
                    Text(
                        text = "最近账单",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (state.recentTransactions.isEmpty()) {
                    item {
                        EmptyTransactionsCard()
                    }
                } else {
                    items(state.recentTransactions, key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            categories = state.categories,
                            accounts = state.accounts,
                            onEdit = { viewModel.editTransaction(transaction) },
                            onDelete = { viewModel.deleteTransaction(transaction.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(state: AccountingUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                text = "奶龙记账",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "当前账本：${state.currentLedger?.name ?: "加载中"}",
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "Milestone 3：首页统计与预算",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ExpenseAnalysisCard(state: AccountingUiState) {
    val summary = state.monthlySummary
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "消费分析",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (summary == null || state.monthlyTransactions.isEmpty()) {
                Text(
                    text = "当前月份还没有足够账单，先记录几笔后再看分析。",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                IncomeExpenseComparison(
                    incomeInCents = summary.incomeInCents,
                    expenseInCents = summary.expenseInCents,
                )
                CategoryExpenseSection(state.categoryExpenseAnalysis)
                DailyExpenseTrendSection(state.dailyExpenseTrend)
            }
        }
    }
}

@Composable
private fun IncomeExpenseComparison(
    incomeInCents: Long,
    expenseInCents: Long,
) {
    val maxAmount = maxOf(incomeInCents, expenseInCents).takeIf { it > 0 } ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "收支对比",
            fontWeight = FontWeight.Bold,
        )
        AnalysisBar(
            label = "收入",
            value = formatCents(incomeInCents),
            progress = incomeInCents.toDouble() / maxAmount.toDouble(),
        )
        AnalysisBar(
            label = "支出",
            value = formatCents(expenseInCents),
            progress = expenseInCents.toDouble() / maxAmount.toDouble(),
        )
    }
}

@Composable
private fun CategoryExpenseSection(items: List<CategoryExpenseAnalysis>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "分类占比",
            fontWeight = FontWeight.Bold,
        )
        if (items.isEmpty()) {
            Text(
                text = "本月还没有支出分类数据。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            items.take(6).forEach { item ->
                AnalysisBar(
                    label = item.categoryName,
                    value = "${formatCents(item.amountInCents)} · ${"%.1f".format(item.percentage)}%",
                    progress = item.percentage / 100.0,
                )
            }
        }
    }
}

@Composable
private fun DailyExpenseTrendSection(points: List<DailyExpensePoint>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "每日支出趋势",
            fontWeight = FontWeight.Bold,
        )
        if (points.isEmpty()) {
            Text(
                text = "本月还没有支出趋势数据。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            points.takeLast(10).forEach { point ->
                AnalysisBar(
                    label = point.dayLabel,
                    value = formatCents(point.amountInCents),
                    progress = point.percentageOfMax / 100.0,
                )
            }
        }
    }
}

@Composable
private fun AnalysisBar(
    label: String,
    value: String,
    progress: Double,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { progress.toFloat().coerceIn(0f, 1f) },
        )
    }
}

@Composable
private fun MonthlySummaryCard(
    state: AccountingUiState,
    onBudgetAmountChange: (String) -> Unit,
    onSaveBudget: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onCategoryBudgetAmountChange: (String) -> Unit,
    onBudgetCategorySelected: (String?) -> Unit,
    onSaveCategoryBudget: () -> Unit,
) {
    val summary = state.monthlySummary
    val budgetUsage = state.budgetUsage
    val progress = ((budgetUsage.usageRate ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${state.currentPeriod} 月度概览",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(onClick = onPreviousMonth) {
                    Text("上月")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onCurrentMonth) {
                    Text("本月")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onNextMonth) {
                    Text("下月")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "支出",
                    value = formatCents(summary?.expenseInCents ?: 0),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "收入",
                    value = formatCents(summary?.incomeInCents ?: 0),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "结余",
                    value = formatCents(summary?.balanceInCents ?: 0),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = budgetStatusText(budgetUsage.status),
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progress },
            )
            Text(
                text = "预算：${budgetUsage.budgetAmountInCents?.let(::formatCents) ?: "未设置"} · " +
                    "已用：${formatCents(budgetUsage.usedAmountInCents)} · " +
                    "剩余：${budgetUsage.remainingAmountInCents?.let(::formatCents) ?: "--"}",
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.budgetAmountText,
                    onValueChange = onBudgetAmountChange,
                    label = { Text("本月预算") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onSaveBudget) {
                    Text("保存预算")
                }
            }
            CategoryBudgetSection(
                state = state,
                onCategoryBudgetAmountChange = onCategoryBudgetAmountChange,
                onBudgetCategorySelected = onBudgetCategorySelected,
                onSaveCategoryBudget = onSaveCategoryBudget,
            )
        }
    }
}

@Composable
private fun CategoryBudgetSection(
    state: AccountingUiState,
    onCategoryBudgetAmountChange: (String) -> Unit,
    onBudgetCategorySelected: (String?) -> Unit,
    onSaveCategoryBudget: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "分类预算",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        CategoryDropdown(
            categories = state.expenseCategories,
            selectedCategoryId = state.selectedBudgetCategoryId,
            onSelected = onBudgetCategorySelected,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.categoryBudgetAmountText,
                onValueChange = onCategoryBudgetAmountChange,
                label = { Text("分类预算金额") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onSaveCategoryBudget) {
                Text("保存分类预算")
            }
        }
        if (state.categoryBudgetUsages.isEmpty()) {
            Text(
                text = "还没有分类预算，可以先给餐饮、交通等高频分类设置预算。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            state.categoryBudgetUsages.forEach { usage ->
                CategoryBudgetRow(usage)
            }
        }
    }
}

@Composable
private fun CategoryBudgetRow(usage: CategoryBudgetUsage) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = usage.categoryName,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = budgetStatusText(usage.status),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { (usage.usageRate / 100.0).toFloat().coerceIn(0f, 1f) },
        )
        Text(
            text = "预算：${formatCents(usage.budgetAmountInCents)} · " +
                "已用：${formatCents(usage.usedAmountInCents)} · " +
                "剩余：${formatCents(usage.remainingAmountInCents)}",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun TransactionForm(
    state: AccountingUiState,
    onTypeSelected: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onAccountSelected: (String?) -> Unit,
    onTargetAccountSelected: (String?) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (state.editingTransactionId == null) "记一笔" else "编辑账单",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            TypeSelector(
                selectedType = state.selectedType,
                onTypeSelected = onTypeSelected,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text("金额") },
                singleLine = true,
            )
            if (state.selectedType != TransactionType.Transfer) {
                CategoryDropdown(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onSelected = onCategorySelected,
                )
            }
            AccountDropdown(
                label = if (state.selectedType == TransactionType.Transfer) "转出账户" else "账户",
                accounts = state.accounts,
                selectedAccountId = state.selectedAccountId,
                onSelected = onAccountSelected,
            )
            if (state.selectedType == TransactionType.Transfer) {
                AccountDropdown(
                    label = "转入账户",
                    accounts = state.accounts,
                    selectedAccountId = state.selectedTargetAccountId,
                    onSelected = onTargetAccountSelected,
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.noteText,
                onValueChange = onNoteChange,
                label = { Text("备注，可选") },
                singleLine = true,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave,
            ) {
                Text(if (state.editingTransactionId == null) "保存" else "保存修改")
            }
            if (state.editingTransactionId != null) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCancelEdit,
                ) {
                    Text("取消编辑")
                }
            }
        }
    }
}

@Composable
private fun TypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransactionType.entries.forEach { type ->
            val label = when (type) {
                TransactionType.Expense -> "支出"
                TransactionType.Income -> "收入"
                TransactionType.Transfer -> "转账"
            }
            if (type == selectedType) {
                Button(onClick = { onTypeSelected(type) }) {
                    Text(label)
                }
            } else {
                OutlinedButton(onClick = { onTypeSelected(type) }) {
                    Text(label)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            readOnly = true,
            value = selected?.name ?: "请选择分类",
            onValueChange = {},
            label = { Text("分类") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelected(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedAccountId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            readOnly = true,
            value = selected?.name ?: "请选择账户",
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onSelected(account.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Text(
            modifier = Modifier.padding(18.dp),
            text = "还没有账单，先记下第一笔吧。",
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = categories.firstOrNull { it.id == transaction.categoryId }
    val account = accounts.firstOrNull { it.id == transaction.accountId }
    val typeLabel = when (transaction.type) {
        TransactionType.Expense -> "支出"
        TransactionType.Income -> "收入"
        TransactionType.Transfer -> "转账"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$typeLabel · ${category?.name ?: "账户转账"}",
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${account?.name ?: "账户"} · ${formatDate(transaction.date)}",
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = formatAmount(transaction),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(onClick = onEdit) {
                Text("编辑")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onDelete) {
                Text("删除")
            }
        }
    }
}

private fun formatAmount(transaction: Transaction): String {
    val prefix = when (transaction.type) {
        TransactionType.Expense -> "-"
        TransactionType.Income -> "+"
        TransactionType.Transfer -> ""
    }
    return "$prefix${formatCents(transaction.amountInCents)}"
}

private fun formatCents(cents: Long): String =
    "¥${"%.2f".format(cents / 100.0)}"

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))

private fun budgetStatusText(status: BudgetStatus): String =
    when (status) {
        BudgetStatus.NotSet -> "未设置"
        BudgetStatus.Normal -> "正常"
        BudgetStatus.Warning -> "接近超支"
        BudgetStatus.Exceeded -> "已超支"
    }
