package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["actorId", "createdAt"]),
        Index(value = ["action"]),
        Index(value = ["targetId"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    val id: String,
    val actorId: String,
    val actorUsername: String,
    val action: String, // LOGIN, LOGOUT, REGISTER, REWARD_VERIFIED, REWARD_DUPLICATE, WITHDRAWAL_CREATE, etc.
    val targetType: String, // USER, REWARD, WITHDRAWAL, SYSTEM
    val targetId: String,
    val metadata: String,
    val createdAt: Long = System.currentTimeMillis()
)
