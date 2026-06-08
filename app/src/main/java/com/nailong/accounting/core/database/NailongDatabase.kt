package com.nailong.accounting.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nailong.accounting.data.local.dao.AccountDao
import com.nailong.accounting.data.local.dao.AiAnalysisReportDao
import com.nailong.accounting.data.local.dao.AppSettingDao
import com.nailong.accounting.data.local.dao.BudgetDao
import com.nailong.accounting.data.local.dao.CategoryDao
import com.nailong.accounting.data.local.dao.LedgerDao
import com.nailong.accounting.data.local.dao.TransactionDao
import com.nailong.accounting.data.local.entity.AccountEntity
import com.nailong.accounting.data.local.entity.AiAnalysisReportEntity
import com.nailong.accounting.data.local.entity.AppSettingEntity
import com.nailong.accounting.data.local.entity.BudgetEntity
import com.nailong.accounting.data.local.entity.CategoryEntity
import com.nailong.accounting.data.local.entity.LedgerEntity
import com.nailong.accounting.data.local.entity.TransactionEntity

@Database(
    entities = [
        LedgerEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        AiAnalysisReportEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NailongDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun aiAnalysisReportDao(): AiAnalysisReportDao
    abstract fun appSettingDao(): AppSettingDao
}
