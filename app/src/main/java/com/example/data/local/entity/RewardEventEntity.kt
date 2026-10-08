package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reward_events",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["userId", "createdAt"])
    ]
)
data class RewardEventEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val idempotencyKey: String,
    val status: String, // STARTED, VERIFIED, REJECTED, EXPIRED, DUPLICATE
    val rewardAmount: Long,
    val verificationStatus: String, // DEV_VERIFIED, ADMOB_VERIFIED, REJECTED, UNVERIFIED
    val riskInfo: String,
    val createdAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
)
