package com.nailong.accounting.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["ledgerId", "date"]),
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val type: String,
    val amount: Long,
    val categoryId: String?,
    val accountId: String,
    val targetAccountId: String?,
    val date: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
