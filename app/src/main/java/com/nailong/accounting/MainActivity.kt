package com.nailong.accounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.nailong.accounting.domain.model.AiAnalysisReport
import com.nailong.accounting.domain.model.AiReportStatus
import com.nailong.accounting.domain.model.BudgetStatus
import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.CategoryBudgetUsage
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

private enum class AppTab(val label: String) {
    Home("首页"),
    Record("记账"),
    Budget("预算"),
    Analysis("分析"),
    Settings("设置"),
}

@Composable
private fun NailongApp(viewModel: AccountingViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(AppTab.Home) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        icon = { Text(tab.label.first().toString()) },
                    )
                }
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppTabContent(
                tab = selectedTab,
                state = state,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun AppTabContent(
    tab: AppTab,
    state: AccountingUiState,
    viewModel: AccountingViewModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (tab) {
            AppTab.Home -> {
                item { HeaderCard(state) }
                item {
                    LedgerManagementCard(
                        state = state,
                        onLedgerNameChange = viewModel::updateLedgerName,
                        onCreateLedger = viewModel::createLedger,
                        onSelectLedger = viewModel::selectLedger,
                    )
                }
                item {
                    HomeOverviewCard(
                        state = state,
                        onPreviousMonth = { viewModel.movePeriod(-1) },
                        onNextMonth = { viewModel.movePeriod(1) },
                        onCurrentMonth = viewModel::goToCurrentPeriod,
                    )
                }
                recentTransactionsSection(state, viewModel)
            }

            AppTab.Record -> {
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
                recentTransactionsSection(state, viewModel)
            }

            AppTab.Budget -> {
                item {
                    BudgetManagementCard(
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
            }

            AppTab.Analysis -> {
                item {
                    PeriodSwitchCard(
                        period = state.currentPeriod,
                        onPreviousMonth = { viewModel.movePeriod(-1) },
                        onNextMonth = { viewModel.movePeriod(1) },
                        onCurrentMonth = viewModel::goToCurrentPeriod,
                    )
                }
                item { ExpenseAnalysisCard(state) }
                item {
                    AiReportCard(
                        state = state,
                        onGenerate = { viewModel.generateAiReport(forceRefresh = false) },
                        onRegenerate = { viewModel.generateAiReport(forceRefresh = true) },
                    )
                }
            }

            AppTab.Settings -> {
                item { SettingsHeaderCard() }
                item { SettingsLedgerCard(state) }
                item {
                    SettingsAccountCard(
                        state = state,
                        onAccountNameChange = viewModel::updateAccountName,
                        onCreateAccount = viewModel::createAccount,
                    )
                }
                item {
                    SettingsAiCard(
                        state = state,
                        onAiBaseUrlChange = viewModel::updateAiBaseUrl,
                        onSaveAiBaseUrl = viewModel::saveAiBaseUrl,
                        onResetAiBaseUrl = viewModel::resetAiBaseUrl,
                    )
                }
                item { SettingsPrivacyCard() }
                item { SettingsAboutCard() }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recentTransactionsSection(
    state: AccountingUiState,
    viewModel: AccountingViewModel,
) {
    item {
        Text(
            text = "最近账单",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    if (state.recentTransactions.isEmpty()) {
        item { EmptyTransactionsCard() }
    } else {
        items(state.recentTransactions, key = { it.id }) { transaction ->
            TransactionRow(
                transaction = transaction,
                categories = state.categories + state.expenseCategories,
                accounts = state.accounts,
                onEdit = { viewModel.editTransaction(transaction) },
                onDelete = { viewModel.deleteTransaction(transaction.id) },
            )
        }
    }
}

@Composable
private fun LedgerManagementCard(
    state: AccountingUiState,
    onLedgerNameChange: (String) -> Unit,
    onCreateLedger: () -> Unit,
    onSelectLedger: (String) -> Unit,
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
                text = "账本",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (state.ledgers.isEmpty()) {
                Text(
                    text = "暂无账本，创建一个后即可开始记账。",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                state.ledgers.forEach { ledger ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ledger.name,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (ledger.id == state.currentLedger?.id) "当前账本" else "可切换",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                            )
                        }
                        if (ledger.id == state.currentLedger?.id) {
                            Text(
                                text = "已选",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            OutlinedButton(onClick = { onSelectLedger(ledger.id) }) {
                                Text("切换")
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.ledgerNameText,
                    onValueChange = onLedgerNameChange,
                    label = { Text("新账本名称") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onCreateLedger) {
                    Text("新增")
                }
            }
        }
    }
}

@Composable
private fun SettingsHeaderCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "设置",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "管理账本、AI 分析、隐私与应用信息。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SettingsLedgerCard(state: AccountingUiState) {
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
                text = "账本与本地数据",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            SettingInfoRow("当前账本", state.currentLedger?.name ?: "加载中")
            SettingInfoRow("账本数量", "${state.ledgers.size} 个")
            SettingInfoRow("账户数量", "${state.accounts.size} 个")
            SettingInfoRow("本月账单", "${state.monthlyTransactions.size} 笔")
            Text(
                text = "账单、预算、账本和 AI 月报缓存当前均优先保存在本机数据库。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SettingsAccountCard(
    state: AccountingUiState,
    onAccountNameChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
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
                text = "账户管理",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (state.accounts.isEmpty()) {
                Text(
                    text = "暂无账户，新增一个后即可用于记账。",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                state.accounts.forEach { account ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = account.type,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                            )
                        }
                        if (account.id == state.selectedAccountId) {
                            Text(
                                text = "记账默认",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.accountNameText,
                    onValueChange = onAccountNameChange,
                    label = { Text("新账户名称") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onCreateAccount) {
                    Text("新增")
                }
            }
        }
    }
}

@Composable
private fun SettingsAiCard(
    state: AccountingUiState,
    onAiBaseUrlChange: (String) -> Unit,
    onSaveAiBaseUrl: () -> Unit,
    onResetAiBaseUrl: () -> Unit,
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
                text = "AI 分析",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            SettingInfoRow("调用方式", "FastAPI 后端代理")
            SettingInfoRow("当前地址", state.aiBaseUrl)
            SettingInfoRow("默认模型", "deepseek-v4-flash")
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.aiBaseUrlText,
                onValueChange = onAiBaseUrlChange,
                label = { Text("AI 后端地址") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onSaveAiBaseUrl,
                ) {
                    Text("保存地址")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onResetAiBaseUrl,
                ) {
                    Text("恢复默认")
                }
            }
            Text(
                text = "正式使用前需要在后端配置 DeepSeek API Key；Android 端不会保存 API Key。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SettingsPrivacyCard() {
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
                text = "隐私与安全",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            SettingInfoRow("AI 上传内容", "脱敏统计摘要")
            SettingInfoRow("不上传内容", "单笔明细、备注、商家、账户敏感信息")
            SettingInfoRow("发布前要求", "关闭明文网络并配置正式后端域名")
            Text(
                text = "当前调试环境允许访问本机 HTTP 后端，正式发布前需要切换为 HTTPS。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SettingsAboutCard() {
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
                text = "关于奶龙记账",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            SettingInfoRow("版本阶段", "Android 原型编码阶段")
            SettingInfoRow("当前里程碑", "Milestone 9 设置页可编辑配置")
            SettingInfoRow("核心能力", "记账、预算、账本、消费分析、AI 月报")
        }
    }
}

@Composable
private fun SettingInfoRow(
    label: String,
    value: String,
) {
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
                text = "本地记账、预算控制、消费分析和 AI 月报已接入基础闭环。",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeOverviewCard(
    state: AccountingUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
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
            PeriodControls(
                period = state.currentPeriod,
                title = "月度概览",
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCurrentMonth = onCurrentMonth,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric("支出", formatCents(summary?.expenseInCents ?: 0), Modifier.weight(1f))
                SummaryMetric("收入", formatCents(summary?.incomeInCents ?: 0), Modifier.weight(1f))
                SummaryMetric("结余", formatCents(summary?.balanceInCents ?: 0), Modifier.weight(1f))
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
        }
    }
}

@Composable
private fun PeriodSwitchCard(
    period: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            PeriodControls(
                period = period,
                title = "分析月份",
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCurrentMonth = onCurrentMonth,
            )
        }
    }
}

@Composable
private fun PeriodControls(
    period: String,
    title: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = period,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlinedButton(onClick = onPreviousMonth) {
            Text("<")
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onCurrentMonth) {
            Text("本月")
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onNextMonth) {
            Text(">")
        }
    }
}

@Composable
private fun AiReportCard(
    state: AccountingUiState,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
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
                text = "AI 月报",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            when (state.aiReportStatus) {
                AiReportStatus.NotGenerated -> {
                    Text(
                        text = "根据本月脱敏统计摘要生成消费总结、预算提醒和节省建议。",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onGenerate,
                    ) {
                        Text("生成 AI 月报")
                    }
                }

                AiReportStatus.Generating -> {
                    Text(
                        text = "AI 正在分析本月消费，请稍候。",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                AiReportStatus.Generated -> {
                    val report = state.aiReport
                    if (report == null) {
                        Text("暂无 AI 月报")
                    } else {
                        AiReportContent(report)
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRegenerate,
                    ) {
                        Text("重新生成")
                    }
                }

                AiReportStatus.Failed -> {
                    Text(
                        text = state.aiErrorMessage ?: "AI 月报生成失败",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onGenerate,
                    ) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiReportContent(report: AiAnalysisReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReportBlock(title = "本月总结", content = report.summary)
        ReportBlock(title = "主要消费方向", content = report.mainCategories.joinToString("、").ifBlank { "暂无" })
        ReportBlock(title = "异常提醒", content = report.alerts.joinToString("\n").ifBlank { "暂无明显异常" })
        ReportBlock(title = "预算执行", content = report.budgetComment)
        ReportBlock(title = "节省建议", content = report.suggestions.joinToString("\n").ifBlank { "暂无建议" })
        ReportBlock(title = "奶龙鼓励", content = report.encouragement)
        Text(
            text = "模型：${report.model}",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ReportBlock(
    title: String,
    content: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = content,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
private fun BudgetManagementCard(
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
            PeriodControls(
                period = state.currentPeriod,
                title = "预算管理",
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCurrentMonth = onCurrentMonth,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric("支出", formatCents(summary?.expenseInCents ?: 0), Modifier.weight(1f))
                SummaryMetric("收入", formatCents(summary?.incomeInCents ?: 0), Modifier.weight(1f))
                SummaryMetric("结余", formatCents(summary?.balanceInCents ?: 0), Modifier.weight(1f))
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
                    Text("保存")
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
                Text("保存")
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
                Text(if (state.editingTransactionId == null) "保存账单" else "保存修改")
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
    "￥${"%.2f".format(cents / 100.0)}"

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))

private fun budgetStatusText(status: BudgetStatus): String =
    when (status) {
        BudgetStatus.NotSet -> "未设置预算"
        BudgetStatus.Normal -> "预算正常"
        BudgetStatus.Warning -> "接近超支"
        BudgetStatus.Exceeded -> "已超支"
    }
