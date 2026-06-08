package com.nailong.accounting.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["ledgerId", "period", "categoryId"], unique = true),
    ],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val categoryId: String?,
    val period: String,
    val amount: Long,
    val alertEnabled: Boolean,
    val alertThreshold: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
