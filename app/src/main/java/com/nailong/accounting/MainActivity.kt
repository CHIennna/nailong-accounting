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
import com.nailong.accounting.domain.model.Category
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
                text = "Milestone 2：基础记账闭环",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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
                .menuAnchor()
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
                .menuAnchor()
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
    val amount = transaction.amountInCents / 100.0
    val prefix = when (transaction.type) {
        TransactionType.Expense -> "-"
        TransactionType.Income -> "+"
        TransactionType.Transfer -> ""
    }
    return "$prefix¥${"%.2f".format(amount)}"
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))
