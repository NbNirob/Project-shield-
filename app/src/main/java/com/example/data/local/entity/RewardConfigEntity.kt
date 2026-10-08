package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_config")
data class RewardConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val rewardAmount: Long = 10,
    val dailyLimit: Long = 100,
    val cooldownSeconds: Long = 15,
    val isWatchAndEarnEnabled: Boolean = true,
    val isKillSwitchActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "SYSTEM"
)
