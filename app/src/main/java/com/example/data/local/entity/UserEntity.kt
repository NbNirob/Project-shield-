package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val coins: Long = 0,
    val role: String = "USER", // "USER" or "ADMIN"
    val status: String = "ACTIVE", // "ACTIVE", "SUSPENDED", "BANNED", "SAFE_MODE"
    val riskScore: Int = 0, // 0 to 100
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
