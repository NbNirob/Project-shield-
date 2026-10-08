package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShieldDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CoinTransactionEntity
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.RiskEventEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.repository.AdminMetrics
import com.example.repository.ShieldRepository
import com.example.reward.DevelopmentRewardVerifier
import com.example.reward.RewardClaimResult
import com.example.reward.RewardEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    AUTH,
    HOME,
    EARN,
    WITHDRAW,
    HISTORY,
    PROFILE,
    ADMIN
}

enum class AdminTab {
    DASHBOARD,
    USERS,
    REWARDS,
    WITHDRAWALS,
    SECURITY,
    AUDIT_LOGS
}

class ShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ShieldDatabase.getInstance(application)
    private val dao = db.shieldDao()
    private val rewardEngine = RewardEngine(dao, DevelopmentRewardVerifier())
    private val repository = ShieldRepository(dao, rewardEngine)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.AUTH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminTab.DASHBOARD)
    val adminTab: StateFlow<AdminTab> = _adminTab.asStateFlow()

    private val _todayEarned = MutableStateFlow(0L)
    val todayEarned: StateFlow<Long> = _todayEarned.asStateFlow()

    private val _isAdPlaying = MutableStateFlow(false)
    val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

    private val _adProgressSeconds = MutableStateFlow(0)
    val adProgressSeconds: StateFlow<Int> = _adProgressSeconds.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _adminMetrics = MutableStateFlow<AdminMetrics?>(null)
    val adminMetrics: StateFlow<AdminMetrics?> = _adminMetrics.asStateFlow()

    private var adSimulationJob: Job? = null

    val config: StateFlow<RewardConfigEntity?> = repository.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalEntity>> = repository.getAllWithdrawalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRiskEvents: StateFlow<List<RiskEventEntity>> = repository.getAllRiskEventsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userTransactions = MutableStateFlow<List<CoinTransactionEntity>>(emptyList())
    val userTransactions: StateFlow<List<CoinTransactionEntity>> = _userTransactions.asStateFlow()

    private val _userWithdrawals = MutableStateFlow<List<WithdrawalEntity>>(emptyList())
    val userWithdrawals: StateFlow<List<WithdrawalEntity>> = _userWithdrawals.asStateFlow()

    private val _userRiskEvents = MutableStateFlow<List<RiskEventEntity>>(emptyList())
    val userRiskEvents: StateFlow<List<RiskEventEntity>> = _userRiskEvents.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            // Auto-login demo user for immediate pleasant preview, or show AUTH
            val demo = dao.getUserByUsername("shielduser")
            if (demo != null) {
                _currentUser.value = demo
                _currentScreen.value = AppScreen.HOME
                refreshUserData(demo.id)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        _statusBanner.value = null
        if (screen == AppScreen.ADMIN) {
            refreshAdminMetrics()
        }
    }

    fun setAdminTab(tab: AdminTab) {
        _adminTab.value = tab
    }

    fun clearMessages() {
        _statusBanner.value = null
        _infoMessage.value = null
    }

    private fun refreshUserData(userId: String) {
        viewModelScope.launch {
            val user = repository.refreshUser(userId)
            _currentUser.value = user
            _todayEarned.value = repository.getTodayEarnedCoins(userId)

            launch {
                repository.getUserTransactions(userId).collect {
                    _userTransactions.value = it
                }
            }
            launch {
                repository.getUserWithdrawals(userId).collect {
                    _userWithdrawals.value = it
                }
            }
            launch {
                repository.getUserRiskEvents(userId).collect {
                    _userRiskEvents.value = it
                }
            }
        }
    }

    fun refreshAdminMetrics() {
        viewModelScope.launch {
            _adminMetrics.value = repository.getAdminMetrics()
        }
    }

    // --- Authentication Actions ---

    fun login(usernameOrEmail: String, pass: String) {
        viewModelScope.launch {
            val res = repository.login(usernameOrEmail, pass)
            res.onSuccess { user ->
                _currentUser.value = user
                _currentScreen.value = AppScreen.HOME
                _statusBanner.value = "Welcome back, ${user.username}!"
                refreshUserData(user.id)
            }.onFailure { err ->
                _statusBanner.value = "Login failed: ${err.message}"
            }
        }
    }

    fun register(user: String, email: String, pass: String) {
        viewModelScope.launch {
            val res = repository.register(user, email, pass)
            res.onSuccess { newUser ->
                _currentUser.value = newUser
                _currentScreen.value = AppScreen.HOME
                _statusBanner.value = "Registration successful! Welcome to Project Shield."
                refreshUserData(newUser.id)
            }.onFailure { err ->
                _statusBanner.value = "Registration failed: ${err.message}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _currentUser.value?.let { repository.logout(it) }
            _currentUser.value = null
            _currentScreen.value = AppScreen.AUTH
            _statusBanner.value = "Logged out successfully"
        }
    }

    fun quickSwitchUser(targetUsername: String) {
        viewModelScope.launch {
            val user = dao.getUserByUsername(targetUsername)
            if (user != null) {
                _currentUser.value = user
                _statusBanner.value = "Switched active session to: ${user.username} (${user.role})"
                refreshUserData(user.id)
            }
        }
    }

    // --- Watch & Earn / Ad Simulation ---

    fun startRewardedAd() {
        val user = _currentUser.value ?: return
        val currentCfg = config.value ?: return

        if (currentCfg.isKillSwitchActive) {
            _statusBanner.value = "Kill Switch is Active! Reward engine is paused."
            return
        }

        if (user.status != "ACTIVE") {
            _statusBanner.value = "Earning disabled for account status: ${user.status}"
            return
        }

        if (_isAdPlaying.value) return

        _isAdPlaying.value = true
        _adProgressSeconds.value = 0

        adSimulationJob?.cancel()
        adSimulationJob = viewModelScope.launch {
            val targetDuration = 6 // 6 seconds for smooth realistic rewarded ad simulation
            for (i in 1..targetDuration) {
                delay(1000)
                _adProgressSeconds.value = i
            }

            // Finish ad and submit verified claim
            completeAdWatchAndClaim(user.id, targetDuration)
            _isAdPlaying.value = false
        }
    }

    fun cancelAdWatch() {
        adSimulationJob?.cancel()
        _isAdPlaying.value = false
        _adProgressSeconds.value = 0
        _statusBanner.value = "Ad playback interrupted. No coins granted."
    }

    private suspend fun completeAdWatchAndClaim(userId: String, watchDuration: Int) {
        val eventId = UUID.randomUUID().toString()
        val idempotencyKey = "IDEMP_${userId}_${System.currentTimeMillis()}"
        val clientTimestamp = System.currentTimeMillis()
        val token = "DEV_PROOF_${eventId.take(8)}_HASH_${UUID.randomUUID().toString().take(6)}"

        val result = repository.claimReward(
            userId = userId,
            idempotencyKey = idempotencyKey,
            clientTimestamp = clientTimestamp,
            verificationToken = token,
            extraMetadata = mapOf(
                "watchDurationSec" to watchDuration.toString(),
                "verifier" to "DevelopmentRewardVerifier"
            )
        )

        when (result) {
            is RewardClaimResult.Success -> {
                _infoMessage.value = result.message
                refreshUserData(userId)
            }
            is RewardClaimResult.Rejected -> {
                _statusBanner.value = result.reason
                refreshUserData(userId)
            }
            is RewardClaimResult.Duplicate -> {
                _statusBanner.value = result.reason
                refreshUserData(userId)
            }
        }
    }

    // --- Anti-Duplicate Testing Trigger ---
    fun testReplayAttack(replayKey: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.claimReward(
                userId = user.id,
                idempotencyKey = replayKey,
                clientTimestamp = System.currentTimeMillis(),
                verificationToken = "DEV_PROOF_REPLAY_TEST",
                extraMetadata = mapOf("watchDurationSec" to "6")
            )
            when (result) {
                is RewardClaimResult.Success -> {
                    _infoMessage.value = "First request succeeded: ${result.message}"
                }
                is RewardClaimResult.Duplicate -> {
                    _statusBanner.value = "REPLAY BLOCKED: ${result.reason}"
                }
                is RewardClaimResult.Rejected -> {
                    _statusBanner.value = "Rejected: ${result.reason}"
                }
            }
            refreshUserData(user.id)
        }
    }

    // --- Withdrawals ---

    fun submitWithdrawal(amount: Long, method: String, details: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.requestWithdrawal(user.id, amount, method, details)
            res.onSuccess {
                _infoMessage.value = "Withdrawal request of $amount coins submitted successfully!"
                refreshUserData(user.id)
            }.onFailure { err ->
                _statusBanner.value = err.message ?: "Withdrawal request failed"
            }
        }
    }

    // --- Admin Operations ---

    fun updateRewardConfig(amount: Long, limit: Long, cooldown: Long, isEnabled: Boolean) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.updateRewardConfig(admin, amount, limit, cooldown, isEnabled)
            res.onSuccess {
                _infoMessage.value = "Reward configuration updated successfully."
                refreshAdminMetrics()
            }.onFailure {
                _statusBanner.value = it.message
            }
        }
    }

    fun toggleKillSwitch(activate: Boolean) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.toggleKillSwitch(admin, activate)
            res.onSuccess {
                _infoMessage.value = if (activate) "Global Kill Switch ACTIVATED." else "Global Kill Switch DEACTIVATED."
                refreshAdminMetrics()
            }.onFailure {
                _statusBanner.value = it.message
            }
        }
    }

    fun updateUserStatus(targetUserId: String, newStatus: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.updateUserStatus(admin, targetUserId, newStatus)
            res.onSuccess {
                _infoMessage.value = "User status updated to $newStatus."
                refreshAdminMetrics()
            }.onFailure {
                _statusBanner.value = it.message
            }
        }
    }

    fun adjustUserBalance(targetUserId: String, delta: Long, reason: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.adjustUserBalance(admin, targetUserId, delta, reason)
            res.onSuccess {
                _infoMessage.value = "Balance adjusted by $delta coins (New balance: $it)."
                refreshAdminMetrics()
            }.onFailure {
                _statusBanner.value = it.message
            }
        }
    }

    fun reviewWithdrawal(withdrawalId: String, approve: Boolean, note: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.reviewWithdrawal(admin, withdrawalId, approve, note)
            res.onSuccess {
                _infoMessage.value = if (approve) "Withdrawal approved." else "Withdrawal rejected & coins refunded."
                refreshAdminMetrics()
            }.onFailure {
                _statusBanner.value = it.message
            }
        }
    }
}
