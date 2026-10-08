package com.example.reward

import com.example.data.local.dao.ShieldDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CoinTransactionEntity
import com.example.data.local.entity.RewardEventEntity
import com.example.security.RiskEngine
import java.util.Calendar
import java.util.UUID

sealed class RewardClaimResult {
    data class Success(
        val rewardAmount: Long,
        val newBalance: Long,
        val eventId: String,
        val message: String
    ) : RewardClaimResult()

    data class Rejected(
        val reason: String,
        val status: String = "REJECTED"
    ) : RewardClaimResult()

    data class Duplicate(
        val reason: String = "Duplicate reward request detected. Replay rejected."
    ) : RewardClaimResult()
}

class RewardEngine(
    private val dao: ShieldDao,
    private val verifier: RewardVerifier = DevelopmentRewardVerifier()
) {

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun processRewardClaim(
        userId: String,
        idempotencyKey: String,
        clientTimestamp: Long,
        verificationToken: String,
        extraMetadata: Map<String, String> = emptyMap()
    ): RewardClaimResult {
        val serverNow = System.currentTimeMillis()

        // 1. Fetch user
        val user = dao.getUserById(userId) ?: return RewardClaimResult.Rejected("User not found")

        // 2. Fetch system config
        val config = dao.getConfig() ?: return RewardClaimResult.Rejected("System configuration missing")

        // 3. Global Kill Switch check
        if (config.isKillSwitchActive) {
            return RewardClaimResult.Rejected("Project Shield Kill Switch is active. Reward processing halted.")
        }

        // 4. User Account Status check (SUSPENDED, BANNED, SAFE_MODE)
        when (user.status) {
            "BANNED" -> return RewardClaimResult.Rejected("Account is permanently banned.")
            "SUSPENDED" -> return RewardClaimResult.Rejected("Account is suspended. Earning is disabled.")
            "SAFE_MODE" -> return RewardClaimResult.Rejected("Account is in Safe Mode. Earning is temporarily restricted.")
            "ACTIVE" -> { /* Allowed */ }
            else -> return RewardClaimResult.Rejected("Invalid account status: ${user.status}")
        }

        // 5. Watch & Earn feature toggle check
        if (!config.isWatchAndEarnEnabled) {
            return RewardClaimResult.Rejected("Watch & Earn is currently disabled by administrator.")
        }

        // 6. Anti-Duplicate / Anti-Replay Protection via Idempotency Key
        val existingEvent = dao.getRewardEventByIdempotency(idempotencyKey)
        if (existingEvent != null) {
            // First request already succeeded or is tracked. Second request rejected as DUPLICATE.
            RiskEngine.recordRiskPenalty(
                dao = dao,
                userId = userId,
                penalty = 15,
                reason = "Replay attack / duplicate reward submission with key: $idempotencyKey"
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorId = userId,
                    actorUsername = user.username,
                    action = "REWARD_DUPLICATE",
                    targetType = "REWARD_EVENT",
                    targetId = existingEvent.id,
                    metadata = "Duplicate key: $idempotencyKey",
                    createdAt = serverNow
                )
            )

            return RewardClaimResult.Duplicate()
        }

        // 7. Time Manipulation Protection
        if (!RiskEngine.validateClientTimestamp(clientTimestamp, serverNow)) {
            RiskEngine.recordRiskPenalty(
                dao = dao,
                userId = userId,
                penalty = 25,
                reason = "Abnormal client timestamp skew (Client: $clientTimestamp vs Server: $serverNow)"
            )
            return RewardClaimResult.Rejected("Security validation failed: Abnormal timestamp detected.")
        }

        // 8. Cooldown Interval check
        val lastEventTime = dao.getLastRewardEventTimestamp(userId)
        if (lastEventTime != null) {
            val elapsedSec = (serverNow - lastEventTime) / 1000
            if (elapsedSec < config.cooldownSeconds) {
                RiskEngine.recordRiskPenalty(
                    dao = dao,
                    userId = userId,
                    penalty = 20,
                    reason = "Reward frequency violation (elapsed $elapsedSec s < cooldown ${config.cooldownSeconds} s)"
                )
                return RewardClaimResult.Rejected("Please wait ${config.cooldownSeconds - elapsedSec} seconds before watching another ad.")
            }
        }

        // 9. Server-Enforced Daily Earning Limit check
        val startOfDay = getStartOfDayMillis()
        val todayEarned = dao.getTodayEarnedCoins(userId, startOfDay)
        val rewardAmount = config.rewardAmount // Server determined! Never client-supplied.

        if (todayEarned + rewardAmount > config.dailyLimit) {
            return RewardClaimResult.Rejected(
                "Daily earning limit reached (${todayEarned}/${config.dailyLimit} coins today). Come back tomorrow!"
            )
        }

        // 10. Reward Verification
        val eventId = UUID.randomUUID().toString()
        val verificationResult = verifier.verifyReward(
            eventId = eventId,
            userId = userId,
            clientTimestamp = clientTimestamp,
            verificationToken = verificationToken,
            extraMetadata = extraMetadata
        )

        when (verificationResult) {
            is VerificationResult.Failure -> {
                if (verificationResult.isFraudSignal) {
                    RiskEngine.recordRiskPenalty(
                        dao = dao,
                        userId = userId,
                        penalty = 20,
                        reason = "Failed verification proof: ${verificationResult.reason}"
                    )
                }

                val rejectedEvent = RewardEventEntity(
                    id = eventId,
                    userId = userId,
                    idempotencyKey = idempotencyKey,
                    status = "REJECTED",
                    rewardAmount = rewardAmount,
                    verificationStatus = "FAILED",
                    riskInfo = verificationResult.reason,
                    createdAt = serverNow
                )
                dao.insertRewardEvent(rejectedEvent)

                dao.insertAuditLog(
                    AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        actorId = userId,
                        actorUsername = user.username,
                        action = "REWARD_REJECTED",
                        targetType = "REWARD_EVENT",
                        targetId = eventId,
                        metadata = "Reason: ${verificationResult.reason}",
                        createdAt = serverNow
                    )
                )

                return RewardClaimResult.Rejected("Ad verification failed: ${verificationResult.reason}")
            }

            is VerificationResult.Success -> {
                // 11. Atomic Coin Credit Transaction
                val newBalance = user.coins + rewardAmount
                val verifiedEvent = RewardEventEntity(
                    id = eventId,
                    userId = userId,
                    idempotencyKey = idempotencyKey,
                    status = "VERIFIED",
                    rewardAmount = rewardAmount,
                    verificationStatus = verificationResult.verificationStatus,
                    riskInfo = "NORMAL",
                    createdAt = serverNow,
                    verifiedAt = serverNow
                )

                val tx = CoinTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    amount = rewardAmount,
                    type = "REWARD",
                    referenceId = eventId,
                    description = "Rewarded Ad watch credited (+${rewardAmount} coins)",
                    balanceAfter = newBalance,
                    createdAt = serverNow
                )

                val audit = AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorId = userId,
                    actorUsername = user.username,
                    action = "REWARD_VERIFIED",
                    targetType = "REWARD_EVENT",
                    targetId = eventId,
                    metadata = "Amount: $rewardAmount, Status: ${verificationResult.verificationStatus}",
                    createdAt = serverNow
                )

                dao.executeAtomicRewardCredit(
                    event = verifiedEvent,
                    transaction = tx,
                    newBalance = newBalance,
                    auditLog = audit
                )

                return RewardClaimResult.Success(
                    rewardAmount = rewardAmount,
                    newBalance = newBalance,
                    eventId = eventId,
                    message = "Earned $rewardAmount coins! Verified securely."
                )
            }
        }
    }
}
