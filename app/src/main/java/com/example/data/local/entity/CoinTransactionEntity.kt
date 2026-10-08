package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coin_transactions",
    indices = [
        Index(value = ["userId", "createdAt"]),
        Index(value = ["referenceId"])
    ]
)
data class CoinTransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val amount: Long, // positive for credits, negative for debits
    val type: String, // REWARD, WITHDRAWAL_RESERVE, WITHDRAWAL_REFUND, ADMIN_ADJUSTMENT
    val referenceId: String,
    val description: String,
    val balanceAfter: Long,
    val createdAt: Long = System.currentTimeMillis()
)
