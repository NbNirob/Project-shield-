package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.RewardedAdSimulationDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EarnScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WithdrawScreen
import com.example.ui.theme.ProjectShieldTheme
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ShieldViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ShieldViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProjectShieldTheme {
                ShieldApp(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun ShieldApp(viewModel: ShieldViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val adminTab by viewModel.adminTab.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val todayEarned by viewModel.todayEarned.collectAsStateWithLifecycle()
    val isAdPlaying by viewModel.isAdPlaying.collectAsStateWithLifecycle()
    val adProgress by viewModel.adProgressSeconds.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val userWithdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allWithdrawals by viewModel.allWithdrawals.collectAsStateWithLifecycle()
    val allAuditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val allRiskEvents by viewModel.allRiskEvents.collectAsStateWithLifecycle()
    val adminMetrics by viewModel.adminMetrics.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Rewarded Ad Simulation Overlay Modal
    RewardedAdSimulationDialog(
        isPlaying = isAdPlaying,
        progressSeconds = adProgress,
        onCancel = { viewModel.cancelAdWatch() }
    )

    val navItems = listOf(
        NavItem(AppScreen.HOME, "Home", Icons.Default.Home, "nav_home"),
        NavItem(AppScreen.EARN, "Earn", Icons.Default.PlayCircle, "nav_earn"),
        NavItem(AppScreen.WITHDRAW, "Withdraw", Icons.Default.AccountBalanceWallet, "nav_withdraw"),
        NavItem(AppScreen.HISTORY, "History", Icons.Default.History, "nav_history"),
        NavItem(AppScreen.PROFILE, "Profile", Icons.Default.Person, "nav_profile")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentUser != null && currentScreen != AppScreen.AUTH) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = Color(0xFF111D3D)
                ) {
                    navItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(item.screen) },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = ShieldGoldPrimary,
                                indicatorColor = ShieldGoldPrimary,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }

                    if (currentUser?.role == "ADMIN") {
                        val selected = currentScreen == AppScreen.ADMIN
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(AppScreen.ADMIN) },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                            label = { Text("Admin", fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = ShieldGoldPrimary,
                                indicatorColor = ShieldGoldPrimary,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_admin")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val user = currentUser
            if (user == null || currentScreen == AppScreen.AUTH) {
                AuthScreen(
                    onLogin = { u, p -> viewModel.login(u, p) },
                    onRegister = { u, e, p -> viewModel.register(u, e, p) },
                    onQuickSwitch = { target -> viewModel.quickSwitchUser(target) },
                    statusMessage = statusBanner ?: infoMessage
                )
            } else {
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(
                        user = user,
                        config = config,
                        todayEarned = todayEarned,
                        recentTransactions = userTransactions,
                        onNavigateToEarn = { viewModel.navigateTo(AppScreen.EARN) },
                        onNavigateToWithdraw = { viewModel.navigateTo(AppScreen.WITHDRAW) },
                        onNavigateToHistory = { viewModel.navigateTo(AppScreen.HISTORY) }
                    )
                    AppScreen.EARN -> EarnScreen(
                        user = user,
                        config = config,
                        todayEarned = todayEarned,
                        onStartWatchAd = { viewModel.startRewardedAd() },
                        onTestReplayAttack = { replayKey -> viewModel.testReplayAttack(replayKey) },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                    AppScreen.WITHDRAW -> WithdrawScreen(
                        user = user,
                        withdrawals = userWithdrawals,
                        onSubmitWithdrawal = { amt, method, details ->
                            viewModel.submitWithdrawal(amt, method, details)
                        },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                    AppScreen.HISTORY -> HistoryScreen(
                        transactions = userTransactions,
                        withdrawals = userWithdrawals,
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                    AppScreen.PROFILE -> ProfileScreen(
                        user = user,
                        onNavigateToAdmin = { viewModel.navigateTo(AppScreen.ADMIN) },
                        onLogout = { viewModel.logout() },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                    AppScreen.ADMIN -> {
                        if (user.role == "ADMIN") {
                            AdminDashboardScreen(
                                currentAdmin = user,
                                activeTab = adminTab,
                                onTabSelected = { viewModel.setAdminTab(it) },
                                metrics = adminMetrics,
                                config = config,
                                allUsers = allUsers,
                                allWithdrawals = allWithdrawals,
                                allAuditLogs = allAuditLogs,
                                allRiskEvents = allRiskEvents,
                                onToggleKillSwitch = { viewModel.toggleKillSwitch(it) },
                                onUpdateRewardConfig = { amt, limit, cool, en ->
                                    viewModel.updateRewardConfig(amt, limit, cool, en)
                                },
                                onUpdateUserStatus = { targetId, status ->
                                    viewModel.updateUserStatus(targetId, status)
                                },
                                onAdjustBalance = { targetId, delta, reason ->
                                    viewModel.adjustUserBalance(targetId, delta, reason)
                                },
                                onReviewWithdrawal = { wId, approve, note ->
                                    viewModel.reviewWithdrawal(wId, approve, note)
                                },
                                onRefreshMetrics = { viewModel.refreshAdminMetrics() },
                                onBack = { viewModel.navigateTo(AppScreen.HOME) }
                            )
                        } else {
                            // Unauthorized access safety fallback
                            HomeScreen(
                                user = user,
                                config = config,
                                todayEarned = todayEarned,
                                recentTransactions = userTransactions,
                                onNavigateToEarn = { viewModel.navigateTo(AppScreen.EARN) },
                                onNavigateToWithdraw = { viewModel.navigateTo(AppScreen.WITHDRAW) },
                                onNavigateToHistory = { viewModel.navigateTo(AppScreen.HISTORY) }
                            )
                        }
                    }
                    AppScreen.AUTH -> {
                        // Handled above
                    }
                }
            }
        }
    }
}
