package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.entity.LedgerEntity
import com.nailong.accounting.data.local.entity.AccountEntity
import com.nailong.accounting.data.local.entity.CategoryEntity
import com.nailong.accounting.data.local.entity.TransactionEntity
import com.nailong.accounting.domain.model.Account
import com.nailong.accounting.domain.model.Category
import com.nailong.accounting.domain.model.Ledger
import com.nailong.accounting.domain.model.Transaction
import com.nailong.accounting.domain.model.TransactionType

fun LedgerEntity.toDomain(): Ledger =
    Ledger(
        id = id,
        name = name,
        icon = icon,
        color = color,
        isDefault = isDefault,
        isArchived = isArchived,
    )

fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        name = name,
        type = TransactionType.fromValue(type),
        icon = icon,
        color = color,
        sortOrder = sortOrder,
    )

fun AccountEntity.toDomain(): Account =
    Account(
        id = id,
        name = name,
        type = type,
        initialBalanceInCents = initialBalance,
        icon = icon,
        color = color,
        isDefault = isDefault,
    )

fun TransactionEntity.toDomain(): Transaction =
    Transaction(
        id = id,
        ledgerId = ledgerId,
        type = TransactionType.fromValue(type),
        amountInCents = amount,
        categoryId = categoryId,
        accountId = accountId,
        targetAccountId = targetAccountId,
        date = date,
        note = note,
    )
