package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.dao.AccountDao
import com.nailong.accounting.data.local.dao.AppSettingDao
import com.nailong.accounting.data.local.dao.CategoryDao
import com.nailong.accounting.data.local.dao.LedgerDao
import com.nailong.accounting.data.local.entity.AccountEntity
import com.nailong.accounting.data.local.entity.AppSettingEntity
import com.nailong.accounting.data.local.entity.CategoryEntity
import com.nailong.accounting.data.local.entity.LedgerEntity
import com.nailong.accounting.domain.repository.BootstrapRepository

class DefaultBootstrapRepository(
    private val ledgerDao: LedgerDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val appSettingDao: AppSettingDao,
) : BootstrapRepository {
    override suspend fun initializeDefaultsIfNeeded() {
        val now = System.currentTimeMillis()

        if (ledgerDao.countActiveAndArchived() == 0) {
            ledgerDao.insert(defaultLedger(now))
            appSettingDao.upsert(AppSettingEntity("currentLedgerId", DEFAULT_LEDGER_ID, now))
        }

        if (categoryDao.countActive() == 0) {
            categoryDao.insertAll(defaultCategories(now))
        }

        if (accountDao.countActiveAndArchived() == 0) {
            accountDao.insertAll(defaultAccounts(now))
            appSettingDao.upsert(AppSettingEntity("defaultAccountId", DEFAULT_ACCOUNT_WECHAT_ID, now))
        }
    }

    private fun defaultLedger(now: Long): LedgerEntity =
        LedgerEntity(
            id = DEFAULT_LEDGER_ID,
            name = "日常账本",
            icon = "book",
            color = "#F7C948",
            isDefault = true,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )

    private fun defaultCategories(now: Long): List<CategoryEntity> {
        val expenses = listOf("餐饮", "交通", "购物", "娱乐", "住房", "学习", "医疗", "人情", "旅行", "其他")
        val incomes = listOf("工资", "兼职", "红包", "投资", "退款", "其他")

        return expenses.mapIndexed { index, name ->
            CategoryEntity(
                id = "category-expense-$index",
                name = name,
                type = "expense",
                icon = "category",
                color = "#E85D5D",
                sortOrder = index,
                isSystem = true,
                isHidden = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
        } + incomes.mapIndexed { index, name ->
            CategoryEntity(
                id = "category-income-$index",
                name = name,
                type = "income",
                icon = "income",
                color = "#2EAD6B",
                sortOrder = index,
                isSystem = true,
                isHidden = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
        }
    }

    private fun defaultAccounts(now: Long): List<AccountEntity> =
        listOf(
            AccountEntity(
                id = DEFAULT_ACCOUNT_WECHAT_ID,
                name = "微信",
                type = "wechat",
                initialBalance = 0,
                icon = "wechat",
                color = "#2EAD6B",
                isDefault = true,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
            AccountEntity(
                id = "account-alipay",
                name = "支付宝",
                type = "alipay",
                initialBalance = 0,
                icon = "alipay",
                color = "#4A90E2",
                isDefault = false,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
            AccountEntity(
                id = "account-cash",
                name = "现金",
                type = "cash",
                initialBalance = 0,
                icon = "cash",
                color = "#F7C948",
                isDefault = false,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )

    companion object {
        private const val DEFAULT_LEDGER_ID = "ledger-default"
        private const val DEFAULT_ACCOUNT_WECHAT_ID = "account-wechat"
    }
}
