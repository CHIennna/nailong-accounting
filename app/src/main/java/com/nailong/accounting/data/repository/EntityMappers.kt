package com.nailong.accounting.data.repository

import com.nailong.accounting.data.local.entity.LedgerEntity
import com.nailong.accounting.domain.model.Ledger

fun LedgerEntity.toDomain(): Ledger =
    Ledger(
        id = id,
        name = name,
        icon = icon,
        color = color,
        isDefault = isDefault,
        isArchived = isArchived,
    )
