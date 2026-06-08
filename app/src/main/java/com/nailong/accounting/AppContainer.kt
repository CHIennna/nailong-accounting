package com.nailong.accounting

import android.content.Context
import androidx.room.Room
import com.nailong.accounting.core.database.NailongDatabase
import com.nailong.accounting.data.repository.DefaultAccountRepository
import com.nailong.accounting.data.repository.DefaultBootstrapRepository
import com.nailong.accounting.data.repository.DefaultBudgetRepository
import com.nailong.accounting.data.repository.DefaultCategoryRepository
import com.nailong.accounting.data.repository.DefaultLedgerRepository
import com.nailong.accounting.data.repository.DefaultTransactionRepository
import com.nailong.accounting.domain.usecase.AddTransactionUseCase
import com.nailong.accounting.domain.usecase.DeleteTransactionUseCase
import com.nailong.accounting.domain.usecase.InitializeDefaultDataUseCase
import com.nailong.accounting.domain.usecase.SetCategoryBudgetUseCase
import com.nailong.accounting.domain.usecase.SetMonthlyBudgetUseCase
import com.nailong.accounting.domain.usecase.UpdateTransactionUseCase

class AppContainer(context: Context) {
    private val database: NailongDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            NailongDatabase::class.java,
            "nailong-accounting.db",
        ).build()

    private val ledgerRepository = DefaultLedgerRepository(database.ledgerDao())
    private val categoryRepository = DefaultCategoryRepository(database.categoryDao())
    private val accountRepository = DefaultAccountRepository(database.accountDao())
    private val transactionRepository = DefaultTransactionRepository(database.transactionDao())
    private val budgetRepository = DefaultBudgetRepository(database.budgetDao())

    val initializeDefaultDataUseCase =
        InitializeDefaultDataUseCase(
            DefaultBootstrapRepository(
                ledgerDao = database.ledgerDao(),
                categoryDao = database.categoryDao(),
                accountDao = database.accountDao(),
                appSettingDao = database.appSettingDao(),
            ),
        )

    val accountingViewModelFactory =
        AccountingViewModel.Factory(
            ledgerRepository = ledgerRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            initializeDefaultDataUseCase = initializeDefaultDataUseCase,
            addTransactionUseCase = AddTransactionUseCase(transactionRepository),
            updateTransactionUseCase = UpdateTransactionUseCase(transactionRepository),
            deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository),
            setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepository),
            setCategoryBudgetUseCase = SetCategoryBudgetUseCase(budgetRepository),
        )
}
