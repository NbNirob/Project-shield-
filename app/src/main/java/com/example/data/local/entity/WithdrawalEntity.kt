package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "withdrawals",
    indices = [
        Index(value = ["userId", "createdAt"]),
        Index(value = ["status"])
    ]
)
data class WithdrawalEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val amount: Long,
    val paymentMethod: String, // bKash, Nagad, Rocket, Bank, PayPal, Crypto
    val paymentDetails: String,
    val status: String, // PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED
    val adminNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
