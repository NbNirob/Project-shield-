package com.example.security

import com.example.data.local.dao.ShieldDao
import com.example.data.local.entity.RiskEventEntity
import java.util.UUID

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

object RiskEngine {

    fun getRiskLevel(score: Int): RiskLevel {
        return when {
            score < 30 -> RiskLevel.LOW
            score < 60 -> RiskLevel.MEDIUM
            score < 80 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }
    }

    /**
     * Checks client timestamp against server time.
     * Rejects timestamps skewed into the future (> 30s) or past (> 2 minutes).
     */
    fun validateClientTimestamp(clientTimestamp: Long, serverNow: Long): Boolean {
        val diff = clientTimestamp - serverNow
        // Clock drift allowed within -120s to +30s
        return diff in -120000..30000
    }

    /**
     * Records a risk score penalty and saves a RiskEventEntity.
     * Optionally places user in SAFE_MODE if score reaches CRITICAL (>= 80).
     */
    suspend fun recordRiskPenalty(
        dao: ShieldDao,
        userId: String,
        penalty: Int,
        reason: String
    ): Int {
        val user = dao.getUserById(userId) ?: return 0
        val previousScore = user.riskScore
        val newScore = (previousScore + penalty).coerceIn(0, 100)

        if (newScore != previousScore) {
            val level = getRiskLevel(newScore)
            dao.updateUserRiskScore(userId, newScore, System.currentTimeMillis())

            val event = RiskEventEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                previousScore = previousScore,
                newScore = newScore,
                reason = reason,
                severity = level.name,
                createdAt = System.currentTimeMillis()
            )
            dao.insertRiskEvent(event)

            // If risk reaches CRITICAL and user is still ACTIVE, auto-escalate to SAFE_MODE
            if (newScore >= 80 && user.status == "ACTIVE") {
                dao.updateUserStatus(userId, "SAFE_MODE", System.currentTimeMillis())
            }
        }
        return newScore
    }
}
