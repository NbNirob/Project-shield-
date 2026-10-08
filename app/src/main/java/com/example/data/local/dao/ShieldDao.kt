package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CoinTransactionEntity
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.RewardEventEntity
import com.example.data.local.entity.RiskEventEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WithdrawalEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ShieldDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE id = :id")
    abstract suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    abstract suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    abstract suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    abstract fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertUser(user: UserEntity)

    @Update
    abstract suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status, updatedAt = :timestamp WHERE id = :userId")
    abstract suspend fun updateUserStatus(userId: String, status: String, timestamp: Long)

    @Query("UPDATE users SET riskScore = :riskScore, updatedAt = :timestamp WHERE id = :userId")
    abstract suspend fun updateUserRiskScore(userId: String, riskScore: Int, timestamp: Long)

    @Query("UPDATE users SET coins = :coins, updatedAt = :timestamp WHERE id = :userId")
    abstract suspend fun updateUserCoins(userId: String, coins: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM users")
    abstract suspend fun countUsers(): Int

    @Query("SELECT COUNT(*) FROM users WHERE status = :status")
    abstract suspend fun countUsersByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE riskScore >= 60")
    abstract suspend fun countHighRiskUsers(): Int

    // --- Reward Config ---
    @Query("SELECT * FROM reward_config WHERE id = 1")
    abstract suspend fun getConfig(): RewardConfigEntity?

    @Query("SELECT * FROM reward_config WHERE id = 1")
    abstract fun getConfigFlow(): Flow<RewardConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateConfig(config: RewardConfigEntity)

    // --- Reward Events ---
    @Query("SELECT * FROM reward_events WHERE id = :id")
    abstract suspend fun getRewardEventById(id: String): RewardEventEntity?

    @Query("SELECT * FROM reward_events WHERE idempotencyKey = :key LIMIT 1")
    abstract suspend fun getRewardEventByIdempotency(key: String): RewardEventEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertRewardEvent(event: RewardEventEntity)

    @Update
    abstract suspend fun updateRewardEvent(event: RewardEventEntity)

    @Query("SELECT COALESCE(SUM(rewardAmount), 0) FROM reward_events WHERE userId = :userId AND status = 'VERIFIED' AND createdAt >= :startOfDayTimestamp")
    abstract suspend fun getTodayEarnedCoins(userId: String, startOfDayTimestamp: Long): Long

    @Query("SELECT MAX(createdAt) FROM reward_events WHERE userId = :userId AND status IN ('VERIFIED', 'STARTED')")
    abstract suspend fun getLastRewardEventTimestamp(userId: String): Long?

    @Query("SELECT COUNT(*) FROM reward_events WHERE createdAt >= :startOfDayTimestamp AND status = 'VERIFIED'")
    abstract suspend fun getTodayVerifiedRewardsCount(startOfDayTimestamp: Long): Int

    // --- Coin Transactions ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertTransaction(transaction: CoinTransactionEntity)

    @Query("SELECT * FROM coin_transactions WHERE userId = :userId ORDER BY createdAt DESC")
    abstract fun getTransactionsForUserFlow(userId: String): Flow<List<CoinTransactionEntity>>

    @Query("SELECT * FROM coin_transactions ORDER BY createdAt DESC LIMIT :limit")
    abstract fun getRecentTransactionsFlow(limit: Int): Flow<List<CoinTransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE type = 'REWARD'")
    abstract suspend fun getTotalCoinsIssued(): Long

    // --- Withdrawals ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertWithdrawal(withdrawal: WithdrawalEntity)

    @Update
    abstract suspend fun updateWithdrawal(withdrawal: WithdrawalEntity)

    @Query("SELECT * FROM withdrawals WHERE id = :id")
    abstract suspend fun getWithdrawalById(id: String): WithdrawalEntity?

    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY createdAt DESC")
    abstract fun getWithdrawalsForUserFlow(userId: String): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals ORDER BY createdAt DESC")
    abstract fun getAllWithdrawalsFlow(): Flow<List<WithdrawalEntity>>

    @Query("SELECT COUNT(*) FROM withdrawals WHERE status = 'PENDING'")
    abstract suspend fun countPendingWithdrawals(): Int

    // --- Audit Logs ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    abstract fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>>

    // --- Risk Events ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertRiskEvent(event: RiskEventEntity)

    @Query("SELECT * FROM risk_events WHERE userId = :userId ORDER BY createdAt DESC")
    abstract fun getRiskEventsForUserFlow(userId: String): Flow<List<RiskEventEntity>>

    @Query("SELECT * FROM risk_events ORDER BY createdAt DESC LIMIT :limit")
    abstract fun getAllRiskEventsFlow(limit: Int = 100): Flow<List<RiskEventEntity>>

    // =========================================================================
    // ATOMIC ACID TRANSACTIONS
    // =========================================================================

    /**
     * Atomically credits coins for a verified reward event.
     * Prevents double credits and guarantees transaction audit trail.
     */
    @Transaction
    open suspend fun executeAtomicRewardCredit(
        event: RewardEventEntity,
        transaction: CoinTransactionEntity,
        newBalance: Long,
        auditLog: AuditLogEntity
    ) {
        insertRewardEvent(event)
        updateUserCoins(event.userId, newBalance, System.currentTimeMillis())
        insertTransaction(transaction)
        insertAuditLog(auditLog)
    }

    /**
     * Atomically reserves/deducts coins for a withdrawal submission.
     * Returns true if balance was sufficient and withdrawal created; false otherwise.
     */
    @Transaction
    open suspend fun executeAtomicWithdrawalReservation(
        userId: String,
        amount: Long,
        withdrawal: WithdrawalEntity,
        transactionId: String,
        auditLog: AuditLogEntity
    ): Boolean {
        val user = getUserById(userId) ?: return false
        if (user.coins < amount) return false

        val newBalance = user.coins - amount
        updateUserCoins(userId, newBalance, System.currentTimeMillis())
        insertWithdrawal(withdrawal)

        val tx = CoinTransactionEntity(
            id = transactionId,
            userId = userId,
            amount = -amount,
            type = "WITHDRAWAL_RESERVE",
            referenceId = withdrawal.id,
            description = "Withdrawal request (${withdrawal.paymentMethod}: ${withdrawal.paymentDetails})",
            balanceAfter = newBalance,
            createdAt = System.currentTimeMillis()
        )
        insertTransaction(tx)
        insertAuditLog(auditLog)
        return true
    }

    /**
     * Atomically processes admin rejection of a withdrawal, returning reserved coins.
     */
    @Transaction
    open suspend fun executeAtomicWithdrawalRejection(
        withdrawalId: String,
        adminNote: String,
        refundTransactionId: String,
        auditLog: AuditLogEntity
    ): Boolean {
        val withdrawal = getWithdrawalById(withdrawalId) ?: return false
        if (withdrawal.status != "PENDING") return false

        val user = getUserById(withdrawal.userId) ?: return false
        val newBalance = user.coins + withdrawal.amount
        val now = System.currentTimeMillis()

        updateWithdrawal(withdrawal.copy(status = "REJECTED", adminNote = adminNote, updatedAt = now))
        updateUserCoins(user.id, newBalance, now)

        val refundTx = CoinTransactionEntity(
            id = refundTransactionId,
            userId = user.id,
            amount = withdrawal.amount,
            type = "WITHDRAWAL_REFUND",
            referenceId = withdrawal.id,
            description = "Withdrawal rejected refund: $adminNote",
            balanceAfter = newBalance,
            createdAt = now
        )
        insertTransaction(refundTx)
        insertAuditLog(auditLog)
        return true
    }

    /**
     * Atomically processes admin approval or completion of a withdrawal.
     */
    @Transaction
    open suspend fun executeAtomicWithdrawalStatusUpdate(
        withdrawalId: String,
        newStatus: String,
        adminNote: String?,
        auditLog: AuditLogEntity
    ): Boolean {
        val withdrawal = getWithdrawalById(withdrawalId) ?: return false
        val now = System.currentTimeMillis()
        updateWithdrawal(withdrawal.copy(status = newStatus, adminNote = adminNote, updatedAt = now))
        insertAuditLog(auditLog)
        return true
    }

    /**
     * Administrative balance adjustment with mandatory audit trail.
     */
    @Transaction
    open suspend fun executeAtomicBalanceAdjustment(
        userId: String,
        deltaAmount: Long,
        reason: String,
        adjustmentTxId: String,
        auditLog: AuditLogEntity
    ): Long? {
        val user = getUserById(userId) ?: return null
        val newBalance = (user.coins + deltaAmount).coerceAtLeast(0L)
        val now = System.currentTimeMillis()

        updateUserCoins(userId, newBalance, now)

        val tx = CoinTransactionEntity(
            id = adjustmentTxId,
            userId = userId,
            amount = deltaAmount,
            type = "ADMIN_ADJUSTMENT",
            referenceId = auditLog.id,
            description = "Admin Adjustment: $reason",
            balanceAfter = newBalance,
            createdAt = now
        )
        insertTransaction(tx)
        insertAuditLog(auditLog)
        return newBalance
    }
}
