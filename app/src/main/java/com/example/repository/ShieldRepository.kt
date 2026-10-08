package com.example.repository

import com.example.data.local.dao.ShieldDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CoinTransactionEntity
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.RiskEventEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.reward.RewardClaimResult
import com.example.reward.RewardEngine
import com.example.security.PasswordHasher
import com.example.security.RiskEngine
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class ShieldRepository(
    private val dao: ShieldDao,
    private val rewardEngine: RewardEngine
) {

    suspend fun seedInitialDataIfNeeded() {
        // Seed configuration if not present
        if (dao.getConfig() == null) {
            dao.insertOrUpdateConfig(
                RewardConfigEntity(
                    id = 1,
                    rewardAmount = 10,
                    dailyLimit = 100,
                    cooldownSeconds = 15,
                    isWatchAndEarnEnabled = true,
                    isKillSwitchActive = false,
                    updatedAt = System.currentTimeMillis(),
                    lastUpdatedBy = "SYSTEM"
                )
            )
        }

        // Seed default administrator if not present
        if (dao.getUserByUsername("admin") == null) {
            val adminSalt = PasswordHasher.generateSalt()
            val adminHash = PasswordHasher.hashPassword("ShieldAdmin123!", adminSalt)
            val adminUser = UserEntity(
                id = "admin-root-001",
                username = "admin",
                email = "admin@shield.sec",
                passwordHash = adminHash,
                salt = adminSalt,
                coins = 500,
                role = "ADMIN",
                status = "ACTIVE",
                riskScore = 0,
                createdAt = System.currentTimeMillis()
            )
            dao.insertUser(adminUser)
            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorId = adminUser.id,
                    actorUsername = adminUser.username,
                    action = "INITIAL_SEED",
                    targetType = "USER",
                    targetId = adminUser.id,
                    metadata = "Initialized default administrator",
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        // Seed a standard demo user if not present
        if (dao.getUserByUsername("shielduser") == null) {
            val userSalt = PasswordHasher.generateSalt()
            val userHash = PasswordHasher.hashPassword("UserPass123!", userSalt)
            val demoUser = UserEntity(
                id = "user-demo-002",
                username = "shielduser",
                email = "user@shield.sec",
                passwordHash = userHash,
                salt = userSalt,
                coins = 80,
                role = "USER",
                status = "ACTIVE",
                riskScore = 15,
                createdAt = System.currentTimeMillis()
            )
            dao.insertUser(demoUser)
            dao.insertTransaction(
                CoinTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = demoUser.id,
                    amount = 80,
                    type = "REWARD",
                    referenceId = "SEED_BONUS",
                    description = "Initial verified earnings balance",
                    balanceAfter = 80,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Authentication ---

    suspend fun register(username: String, email: String, password: String): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase()
        val cleanEmail = email.trim().lowercase()

        if (cleanUsername.length < 3) return Result.failure(Exception("Username must be at least 3 characters"))
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) return Result.failure(Exception("Invalid email format"))
        if (password.length < 6) return Result.failure(Exception("Password must be at least 6 characters"))

        if (dao.getUserByUsername(cleanUsername) != null) {
            return Result.failure(Exception("Username already in use"))
        }
        if (dao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(Exception("Email already registered"))
        }

        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)
        val now = System.currentTimeMillis()

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            username = cleanUsername,
            email = cleanEmail,
            passwordHash = hash,
            salt = salt,
            coins = 0,
            role = "USER",
            status = "ACTIVE",
            riskScore = 0,
            createdAt = now,
            updatedAt = now,
            lastLoginAt = now
        )

        dao.insertUser(newUser)

        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = newUser.id,
                actorUsername = newUser.username,
                action = "REGISTER",
                targetType = "USER",
                targetId = newUser.id,
                metadata = "User registered successfully",
                createdAt = now
            )
        )

        return Result.success(newUser)
    }

    suspend fun login(usernameOrEmail: String, password: String): Result<UserEntity> {
        val clean = usernameOrEmail.trim().lowercase()
        val user = if (clean.contains("@")) {
            dao.getUserByEmail(clean)
        } else {
            dao.getUserByUsername(clean)
        } ?: return Result.failure(Exception("Invalid credentials"))

        if (!PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)) {
            // Record failed login risk signal
            RiskEngine.recordRiskPenalty(
                dao = dao,
                userId = user.id,
                penalty = 10,
                reason = "Failed password authentication attempt"
            )
            return Result.failure(Exception("Invalid credentials"))
        }

        val now = System.currentTimeMillis()
        val updatedUser = user.copy(lastLoginAt = now)
        dao.updateUser(updatedUser)

        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = user.id,
                actorUsername = user.username,
                action = "LOGIN",
                targetType = "USER",
                targetId = user.id,
                metadata = "User logged in",
                createdAt = now
            )
        )

        return Result.success(updatedUser)
    }

    suspend fun logout(user: UserEntity) {
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = user.id,
                actorUsername = user.username,
                action = "LOGOUT",
                targetType = "USER",
                targetId = user.id,
                metadata = "User session ended",
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun refreshUser(userId: String): UserEntity? {
        return dao.getUserById(userId)
    }

    // --- Reward Processing ---

    suspend fun claimReward(
        userId: String,
        idempotencyKey: String,
        clientTimestamp: Long,
        verificationToken: String,
        extraMetadata: Map<String, String> = emptyMap()
    ): RewardClaimResult {
        return rewardEngine.processRewardClaim(
            userId = userId,
            idempotencyKey = idempotencyKey,
            clientTimestamp = clientTimestamp,
            verificationToken = verificationToken,
            extraMetadata = extraMetadata
        )
    }

    suspend fun getTodayEarnedCoins(userId: String): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return dao.getTodayEarnedCoins(userId, cal.timeInMillis)
    }

    // --- Withdrawals ---

    suspend fun requestWithdrawal(
        userId: String,
        amount: Long,
        paymentMethod: String,
        paymentDetails: String
    ): Result<WithdrawalEntity> {
        val config = dao.getConfig() ?: return Result.failure(Exception("Configuration not found"))
        if (config.isKillSwitchActive) {
            return Result.failure(Exception("Kill Switch is active. Withdrawals temporarily disabled."))
        }

        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        if (user.status != "ACTIVE") {
            return Result.failure(Exception("Withdrawals restricted for account status: ${user.status}"))
        }

        if (amount < 20) {
            return Result.failure(Exception("Minimum withdrawal is 20 coins"))
        }

        if (user.coins < amount) {
            return Result.failure(Exception("Insufficient balance ($amount requested, ${user.coins} available)"))
        }

        if (paymentDetails.trim().length < 5) {
            return Result.failure(Exception("Please enter valid account/wallet details"))
        }

        val now = System.currentTimeMillis()
        val withdrawalId = UUID.randomUUID().toString()
        val txId = UUID.randomUUID().toString()

        val withdrawal = WithdrawalEntity(
            id = withdrawalId,
            userId = userId,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentDetails = paymentDetails.trim(),
            status = "PENDING",
            adminNote = null,
            createdAt = now,
            updatedAt = now
        )

        val auditLog = AuditLogEntity(
            id = UUID.randomUUID().toString(),
            actorId = userId,
            actorUsername = user.username,
            action = "WITHDRAWAL_CREATE",
            targetType = "WITHDRAWAL",
            targetId = withdrawalId,
            metadata = "Requested $amount coins to $paymentMethod ($paymentDetails)",
            createdAt = now
        )

        val success = dao.executeAtomicWithdrawalReservation(
            userId = userId,
            amount = amount,
            withdrawal = withdrawal,
            transactionId = txId,
            auditLog = auditLog
        )

        return if (success) {
            Result.success(withdrawal)
        } else {
            Result.failure(Exception("Atomic transaction failed. Balance may have changed."))
        }
    }

    // --- Admin Operations ---

    suspend fun updateRewardConfig(
        adminUser: UserEntity,
        newRewardAmount: Long,
        newDailyLimit: Long,
        newCooldownSeconds: Long,
        isEnabled: Boolean
    ): Result<Unit> {
        if (adminUser.role != "ADMIN") return Result.failure(Exception("Unauthorized: Admin role required"))

        val currentConfig = dao.getConfig() ?: return Result.failure(Exception("Config not found"))
        val now = System.currentTimeMillis()

        val auditMetadata = "Config update: RewardAmount(${currentConfig.rewardAmount}->${newRewardAmount}), " +
                "DailyLimit(${currentConfig.dailyLimit}->${newDailyLimit}), " +
                "Cooldown(${currentConfig.cooldownSeconds}->${newCooldownSeconds}), " +
                "WatchAndEarnEnabled(${currentConfig.isWatchAndEarnEnabled}->${isEnabled})"

        val updated = currentConfig.copy(
            rewardAmount = newRewardAmount,
            dailyLimit = newDailyLimit,
            cooldownSeconds = newCooldownSeconds,
            isWatchAndEarnEnabled = isEnabled,
            updatedAt = now,
            lastUpdatedBy = adminUser.username
        )

        dao.insertOrUpdateConfig(updated)

        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = adminUser.id,
                actorUsername = adminUser.username,
                action = "REWARD_CONFIG_CHANGE",
                targetType = "SYSTEM_CONFIG",
                targetId = "1",
                metadata = auditMetadata,
                createdAt = now
            )
        )

        return Result.success(Unit)
    }

    suspend fun toggleKillSwitch(adminUser: UserEntity, activate: Boolean): Result<Unit> {
        if (adminUser.role != "ADMIN") return Result.failure(Exception("Unauthorized: Admin role required"))

        val currentConfig = dao.getConfig() ?: return Result.failure(Exception("Config not found"))
        val now = System.currentTimeMillis()

        val updated = currentConfig.copy(
            isKillSwitchActive = activate,
            updatedAt = now,
            lastUpdatedBy = adminUser.username
        )
        dao.insertOrUpdateConfig(updated)

        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = adminUser.id,
                actorUsername = adminUser.username,
                action = "KILL_SWITCH_TOGGLE",
                targetType = "SYSTEM_CONFIG",
                targetId = "1",
                metadata = "Kill switch set to: $activate",
                createdAt = now
            )
        )

        return Result.success(Unit)
    }

    suspend fun updateUserStatus(
        adminUser: UserEntity,
        targetUserId: String,
        newStatus: String
    ): Result<Unit> {
        if (adminUser.role != "ADMIN") return Result.failure(Exception("Unauthorized: Admin role required"))
        val target = dao.getUserById(targetUserId) ?: return Result.failure(Exception("Target user not found"))

        val now = System.currentTimeMillis()
        dao.updateUserStatus(targetUserId, newStatus, now)

        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = adminUser.id,
                actorUsername = adminUser.username,
                action = when (newStatus) {
                    "SUSPENDED" -> "ACCOUNT_SUSPEND"
                    "BANNED" -> "ACCOUNT_BAN"
                    "SAFE_MODE" -> "SAFE_MODE_TOGGLE"
                    else -> "ACCOUNT_STATUS_CHANGE"
                },
                targetType = "USER",
                targetId = targetUserId,
                metadata = "Status changed from ${target.status} to $newStatus for ${target.username}",
                createdAt = now
            )
        )

        return Result.success(Unit)
    }

    suspend fun adjustUserBalance(
        adminUser: UserEntity,
        targetUserId: String,
        deltaAmount: Long,
        reason: String
    ): Result<Long> {
        if (adminUser.role != "ADMIN") return Result.failure(Exception("Unauthorized: Admin role required"))
        if (reason.trim().length < 4) return Result.failure(Exception("Audit reason is mandatory for balance adjustment"))

        val auditLog = AuditLogEntity(
            id = UUID.randomUUID().toString(),
            actorId = adminUser.id,
            actorUsername = adminUser.username,
            action = "BALANCE_ADJUSTMENT",
            targetType = "USER",
            targetId = targetUserId,
            metadata = "Delta: $deltaAmount, Reason: $reason",
            createdAt = System.currentTimeMillis()
        )

        val newBalance = dao.executeAtomicBalanceAdjustment(
            userId = targetUserId,
            deltaAmount = deltaAmount,
            reason = reason,
            adjustmentTxId = UUID.randomUUID().toString(),
            auditLog = auditLog
        ) ?: return Result.failure(Exception("User not found"))

        return Result.success(newBalance)
    }

    suspend fun reviewWithdrawal(
        adminUser: UserEntity,
        withdrawalId: String,
        approve: Boolean,
        adminNote: String
    ): Result<Unit> {
        if (adminUser.role != "ADMIN") return Result.failure(Exception("Unauthorized: Admin role required"))
        val withdrawal = dao.getWithdrawalById(withdrawalId) ?: return Result.failure(Exception("Withdrawal not found"))
        val now = System.currentTimeMillis()

        if (approve) {
            val auditLog = AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = adminUser.id,
                actorUsername = adminUser.username,
                action = "WITHDRAWAL_APPROVE",
                targetType = "WITHDRAWAL",
                targetId = withdrawalId,
                metadata = "Approved payout of ${withdrawal.amount} coins. Note: $adminNote",
                createdAt = now
            )
            val success = dao.executeAtomicWithdrawalStatusUpdate(
                withdrawalId = withdrawalId,
                newStatus = "COMPLETED",
                adminNote = adminNote,
                auditLog = auditLog
            )
            return if (success) Result.success(Unit) else Result.failure(Exception("Failed to update status"))
        } else {
            val refundTxId = UUID.randomUUID().toString()
            val auditLog = AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = adminUser.id,
                actorUsername = adminUser.username,
                action = "WITHDRAWAL_REJECT",
                targetType = "WITHDRAWAL",
                targetId = withdrawalId,
                metadata = "Rejected: ${withdrawal.amount} coins returned to user. Note: $adminNote",
                createdAt = now
            )
            val success = dao.executeAtomicWithdrawalRejection(
                withdrawalId = withdrawalId,
                adminNote = adminNote,
                refundTransactionId = refundTxId,
                auditLog = auditLog
            )
            return if (success) Result.success(Unit) else Result.failure(Exception("Failed to refund withdrawal"))
        }
    }

    // --- Reactive Flows ---

    fun getUserTransactions(userId: String): Flow<List<CoinTransactionEntity>> =
        dao.getTransactionsForUserFlow(userId)

    fun getUserWithdrawals(userId: String): Flow<List<WithdrawalEntity>> =
        dao.getWithdrawalsForUserFlow(userId)

    fun getUserRiskEvents(userId: String): Flow<List<RiskEventEntity>> =
        dao.getRiskEventsForUserFlow(userId)

    fun getConfigFlow(): Flow<RewardConfigEntity?> = dao.getConfigFlow()

    fun getAllUsersFlow(): Flow<List<UserEntity>> = dao.getAllUsersFlow()

    fun getAllWithdrawalsFlow(): Flow<List<WithdrawalEntity>> = dao.getAllWithdrawalsFlow()

    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>> = dao.getAllAuditLogsFlow()

    fun getAllRiskEventsFlow(): Flow<List<RiskEventEntity>> = dao.getAllRiskEventsFlow()

    suspend fun getAdminMetrics(): AdminMetrics {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return AdminMetrics(
            totalUsers = dao.countUsers(),
            activeUsers = dao.countUsersByStatus("ACTIVE"),
            suspendedUsers = dao.countUsersByStatus("SUSPENDED"),
            bannedUsers = dao.countUsersByStatus("BANNED"),
            totalCoinsIssued = dao.getTotalCoinsIssued(),
            todayRewardsCount = dao.getTodayVerifiedRewardsCount(startOfDay),
            pendingWithdrawals = dao.countPendingWithdrawals(),
            highRiskUsers = dao.countHighRiskUsers()
        )
    }
}

data class AdminMetrics(
    val totalUsers: Int,
    val activeUsers: Int,
    val suspendedUsers: Int,
    val bannedUsers: Int,
    val totalCoinsIssued: Long,
    val todayRewardsCount: Int,
    val pendingWithdrawals: Int,
    val highRiskUsers: Int
)
