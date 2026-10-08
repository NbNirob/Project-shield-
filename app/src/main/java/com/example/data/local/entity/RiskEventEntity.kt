package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "risk_events",
    indices = [
        Index(value = ["userId", "createdAt"]),
        Index(value = ["severity"])
    ]
)
data class RiskEventEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val previousScore: Int,
    val newScore: Int,
    val reason: String,
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val createdAt: Long = System.currentTimeMillis()
)
