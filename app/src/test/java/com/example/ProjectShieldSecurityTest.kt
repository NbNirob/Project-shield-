package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ShieldDatabase
import com.example.data.local.dao.ShieldDao
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.UserEntity
import com.example.repository.ShieldRepository
import com.example.reward.DevelopmentRewardVerifier
import com.example.reward.RewardClaimResult
import com.example.reward.RewardEngine
import com.example.security.PasswordHasher
import com.example.security.RiskEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectShieldSecurityTest {

    private lateinit var db: ShieldDatabase
    private lateinit var dao: ShieldDao
    private lateinit var repository: ShieldRepository
    private lateinit var rewardEngine: RewardEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.shieldDao()
        rewardEngine = RewardEngine(dao, DevelopmentRewardVerifier())
        repository = ShieldRepository(dao, rewardEngine)

        runBlocking {
            repository.seedInitialDataIfNeeded()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 1. Registration works
    @Test
    fun testRegistrationWorks() = runBlocking {
        val result = repository.register("alice", "alice@shield.sec", "StrongPass123!")
        assertTrue("Registration must succeed", result.isSuccess)

        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("alice", user?.username)
        assertNotEquals("StrongPass123!", user?.passwordHash) // Password must be hashed!

        val auditLogs = dao.getAllAuditLogsFlow().first()
        assertTrue(auditLogs.any { it.action == "REGISTER" && it.actorId == user?.id })
    }

    // 2. Login works
    @Test
    fun testLoginWorks() = runBlocking {
        repository.register("bob", "bob@shield.sec", "SecureBobPass123!")
        val loginRes = repository.login("bob", "SecureBobPass123!")
        assertTrue("Login with correct password must succeed", loginRes.isSuccess)

        val badLoginRes = repository.login("bob", "WrongPassword!")
        assertTrue("Login with wrong password must fail", badLoginRes.isFailure)
    }

    // 3. Unauthorized users cannot access admin operations
    @Test
    fun testUnauthorizedUsersCannotAccessAdminAPIs() = runBlocking {
        val normalUser = repository.register("charlie", "charlie@shield.sec", "Pass123!").getOrThrow()
        val updateRes = repository.updateRewardConfig(normalUser, 20, 200, 10, true)
        assertTrue("Normal user must NOT be able to modify reward configuration", updateRes.isFailure)
    }

    // 4. Disabled Watch & Earn rejects rewards
    @Test
    fun testDisabledWatchAndEarnRejectsRewards() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        repository.updateRewardConfig(admin, 10, 100, 15, false) // Disable Watch & Earn

        val user = repository.register("dave", "dave@shield.sec", "Pass123!").getOrThrow()
        val eventId = UUID.randomUUID().toString()
        val claim = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = "IDEMP_${UUID.randomUUID()}",
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${eventId.take(8)}_HASH",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )

        assertTrue("Disabled Watch & Earn must reject reward claim", claim is RewardClaimResult.Rejected)
    }

    // 5. Daily limit is enforced server-side
    @Test
    fun testDailyLimitEnforcedServerSide() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        repository.updateRewardConfig(admin, newRewardAmount = 50, newDailyLimit = 60, newCooldownSeconds = 0, isEnabled = true)

        val user = repository.register("eve", "eve@shield.sec", "Pass123!").getOrThrow()

        // First claim: 50 coins (50 <= 60 daily cap) -> Success
        val ev1 = UUID.randomUUID().toString()
        val claim1 = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = "IDEMP_1",
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${ev1.take(8)}_OK",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )
        assertTrue("First claim within limit must succeed", claim1 is RewardClaimResult.Success)

        // Second claim: +50 coins would make 100 > 60 -> Rejected
        val ev2 = UUID.randomUUID().toString()
        val claim2 = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = "IDEMP_2",
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${ev2.take(8)}_OK",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )
        assertTrue("Claim exceeding daily limit must be rejected", claim2 is RewardClaimResult.Rejected)
    }

    // 6. Unverified rewards cannot add coins
    @Test
    fun testUnverifiedRewardsCannotAddCoins() = runBlocking {
        val user = repository.register("frank", "frank@shield.sec", "Pass123!").getOrThrow()
        val claim = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = "IDEMP_FRANK",
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "INVALID_TOKEN_FAKE",
            extraMetadata = emptyMap()
        )
        assertTrue("Unverified reward proof must be rejected", claim is RewardClaimResult.Rejected)
        val refreshed = dao.getUserById(user.id)
        assertEquals(0L, refreshed?.coins)
    }

    // 7. Duplicate reward cannot add coins twice
    @Test
    fun testDuplicateRewardCannotAddCoinsTwice() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        repository.updateRewardConfig(admin, 10, 100, 0, true)

        val user = repository.register("grace", "grace@shield.sec", "Pass123!").getOrThrow()
        val sameKey = "STATIC_REPLAY_KEY_123"
        val ev = UUID.randomUUID().toString()

        val claim1 = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = sameKey,
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${ev.take(8)}_TOKEN",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )
        assertTrue("First request must succeed", claim1 is RewardClaimResult.Success)

        val claim2 = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = sameKey,
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${ev.take(8)}_TOKEN",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )
        assertTrue("Second request with duplicate idempotency key must be DUPLICATE", claim2 is RewardClaimResult.Duplicate)

        val refreshed = dao.getUserById(user.id)
        assertEquals(10L, refreshed?.coins) // Coins credited only once!
    }

    // 8. Simultaneous/Duplicate protection check
    @Test
    fun testIdempotencyUniqueConstraint() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        repository.updateRewardConfig(admin, 10, 100, 0, true)

        val user = repository.register("heidi", "heidi@shield.sec", "Pass123!").getOrThrow()
        val key = "CONCURRENCY_KEY_TEST"
        val ev = UUID.randomUUID().toString()

        val r1 = rewardEngine.processRewardClaim(user.id, key, System.currentTimeMillis(), "DEV_PROOF_${ev.take(8)}_A", mapOf("watchDurationSec" to "6"))
        val r2 = rewardEngine.processRewardClaim(user.id, key, System.currentTimeMillis(), "DEV_PROOF_${ev.take(8)}_A", mapOf("watchDurationSec" to "6"))

        assertTrue(r1 is RewardClaimResult.Success)
        assertTrue(r2 is RewardClaimResult.Duplicate)
        assertEquals(10L, dao.getUserById(user.id)?.coins)
    }

    // 9. Withdrawal cannot exceed available balance
    @Test
    fun testWithdrawalCannotExceedAvailableBalance() = runBlocking {
        val user = repository.register("ivan", "ivan@shield.sec", "Pass123!").getOrThrow()
        // Ivan has 0 coins, tries to withdraw 50
        val withRes = repository.requestWithdrawal(user.id, 50, "bKash", "01700000000")
        assertTrue("Withdrawal exceeding balance must fail", withRes.isFailure)
    }

    // 10. Rejected withdrawal returns reserved coins
    @Test
    fun testRejectedWithdrawalReturnsReservedCoins() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        val user = repository.register("judy", "judy@shield.sec", "Pass123!").getOrThrow()

        // Give Judy 100 coins via admin adjustment
        repository.adjustUserBalance(admin, user.id, 100, "Initial seed balance")
        assertEquals(100L, dao.getUserById(user.id)?.coins)

        // Request 50 coins withdrawal
        val wRes = repository.requestWithdrawal(user.id, 50, "bKash", "01711111111")
        assertTrue(wRes.isSuccess)
        val withdrawal = wRes.getOrThrow()

        // Coins should now be 50 (reserved)
        assertEquals(50L, dao.getUserById(user.id)?.coins)

        // Admin rejects withdrawal
        val reviewRes = repository.reviewWithdrawal(admin, withdrawal.id, approve = false, adminNote = "Invalid number")
        assertTrue(reviewRes.isSuccess)

        // Judy should have her 50 coins refunded back to 100!
        assertEquals(100L, dao.getUserById(user.id)?.coins)
    }

    // 11. Suspended users cannot earn
    @Test
    fun testSuspendedUsersCannotEarn() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        val user = repository.register("mallory", "mallory@shield.sec", "Pass123!").getOrThrow()

        repository.updateUserStatus(admin, user.id, "SUSPENDED")

        val ev = UUID.randomUUID().toString()
        val claim = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = "IDEMP_MALLORY",
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${ev.take(8)}_OK",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )
        assertTrue("Suspended users must not be able to claim rewards", claim is RewardClaimResult.Rejected)
    }

    // 12. Safe Mode blocks earning
    @Test
    fun testSafeModeBlocksEarning() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        val user = repository.register("oscar", "oscar@shield.sec", "Pass123!").getOrThrow()

        repository.updateUserStatus(admin, user.id, "SAFE_MODE")

        val ev = UUID.randomUUID().toString()
        val claim = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = "IDEMP_OSCAR",
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${ev.take(8)}_OK",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )
        assertTrue("Safe Mode must block earning rewards", claim is RewardClaimResult.Rejected)
    }

    // 13. Kill Switch blocks earning
    @Test
    fun testKillSwitchBlocksEarning() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        repository.toggleKillSwitch(admin, true)

        val user = repository.register("peggy", "peggy@shield.sec", "Pass123!").getOrThrow()
        val ev = UUID.randomUUID().toString()
        val claim = rewardEngine.processRewardClaim(
            userId = user.id,
            idempotencyKey = "IDEMP_PEGGY",
            clientTimestamp = System.currentTimeMillis(),
            verificationToken = "DEV_PROOF_${ev.take(8)}_OK",
            extraMetadata = mapOf("watchDurationSec" to "6")
        )
        assertTrue("Kill switch must block earning", claim is RewardClaimResult.Rejected)
    }

    // 14. Admin changes create audit records
    @Test
    fun testAdminChangesCreateAuditRecords() = runBlocking {
        val admin = dao.getUserByUsername("admin")!!
        repository.updateRewardConfig(admin, 25, 250, 20, true)

        val logs = dao.getAllAuditLogsFlow().first()
        assertTrue("REWARD_CONFIG_CHANGE audit log must exist", logs.any { it.action == "REWARD_CONFIG_CHANGE" })
    }

    // 15. Risk score changes create risk events
    @Test
    fun testRiskScoreChangesCreateRiskEvents() = runBlocking {
        val user = repository.register("trent", "trent@shield.sec", "Pass123!").getOrThrow()
        RiskEngine.recordRiskPenalty(dao, user.id, 25, "Test anomalous frequency")

        val events = dao.getRiskEventsForUserFlow(user.id).first()
        assertTrue("RiskEventEntity must be inserted", events.isNotEmpty())
        assertEquals(25, events.first().newScore)
        assertEquals("Test anomalous frequency", events.first().reason)
    }
}
