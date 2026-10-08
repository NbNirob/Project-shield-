package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.RiskEventEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.repository.AdminMetrics
import com.example.ui.components.RiskBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.theme.ShieldRoseRisk
import com.example.ui.viewmodel.AdminTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    currentAdmin: UserEntity,
    activeTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit,
    metrics: AdminMetrics?,
    config: RewardConfigEntity?,
    allUsers: List<UserEntity>,
    allWithdrawals: List<WithdrawalEntity>,
    allAuditLogs: List<AuditLogEntity>,
    allRiskEvents: List<RiskEventEntity>,
    onToggleKillSwitch: (Boolean) -> Unit,
    onUpdateRewardConfig: (Long, Long, Long, Boolean) -> Unit,
    onUpdateUserStatus: (String, String) -> Unit,
    onAdjustBalance: (String, Long, String) -> Unit,
    onReviewWithdrawal: (String, Boolean, String) -> Unit,
    onRefreshMetrics: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        "PROJECT SHIELD ADMIN",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp,
                        color = ShieldGoldPrimary
                    )
                    Text(
                        "Admin: ${currentAdmin.username} (${currentAdmin.email})",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRefreshMetrics) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Metrics", tint = ShieldGoldPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Admin Sub-Tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AdminTab.values()) { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    label = { Text(tab.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShieldGoldPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TAB CONTENTS
        when (activeTab) {
            AdminTab.DASHBOARD -> AdminOverviewTab(
                metrics = metrics,
                config = config,
                onToggleKillSwitch = onToggleKillSwitch
            )
            AdminTab.USERS -> AdminUsersTab(
                users = allUsers,
                onUpdateStatus = onUpdateUserStatus,
                onAdjustBalance = onAdjustBalance
            )
            AdminTab.REWARDS -> AdminRewardsTab(
                config = config,
                onSaveConfig = onUpdateRewardConfig
            )
            AdminTab.WITHDRAWALS -> AdminWithdrawalsTab(
                withdrawals = allWithdrawals,
                onReview = onReviewWithdrawal
            )
            AdminTab.SECURITY -> AdminSecurityTab(
                riskEvents = allRiskEvents,
                highRiskUsers = allUsers.filter { it.riskScore >= 60 || it.status == "SAFE_MODE" },
                onUpdateStatus = onUpdateUserStatus
            )
            AdminTab.AUDIT_LOGS -> AdminAuditLogsTab(
                logs = allAuditLogs,
                dateFormat = dateFormat
            )
        }
    }
}

@Composable
fun AdminOverviewTab(
    metrics: AdminMetrics?,
    config: RewardConfigEntity?,
    onToggleKillSwitch: (Boolean) -> Unit
) {
    val isKillSwitch = config?.isKillSwitchActive == true

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Global Kill Switch Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isKillSwitch) ShieldRoseRisk.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (isKillSwitch) ShieldRoseRisk else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (isKillSwitch) ShieldRoseRisk else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "GLOBAL KILL SWITCH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isKillSwitch) ShieldRoseRisk else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isKillSwitch) "ACTIVE: Earning & withdrawals halted system-wide."
                                else "STANDBY: Normal reward & withdrawal operations active.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isKillSwitch,
                        onCheckedChange = onToggleKillSwitch,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ShieldRoseRisk,
                            checkedTrackColor = ShieldRoseRisk.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("admin_kill_switch")
                    )
                }
            }
        }

        // Metrics Grid
        item {
            Text("Platform Statistics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Total Users",
                    value = "${metrics?.totalUsers ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Users",
                    value = "${metrics?.activeUsers ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = ShieldEmeraldSafe
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Suspended",
                    value = "${metrics?.suspendedUsers ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF59E0B)
                )
                MetricCard(
                    title = "Banned",
                    value = "${metrics?.bannedUsers ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = ShieldRoseRisk
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Coins Issued",
                    value = "${metrics?.totalCoinsIssued ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = ShieldGoldPrimary
                )
                MetricCard(
                    title = "Today Rewards",
                    value = "${metrics?.todayRewardsCount ?: 0}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Pending Withdrawals",
                    value = "${metrics?.pendingWithdrawals ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = if ((metrics?.pendingWithdrawals ?: 0) > 0) Color(0xFFF59E0B) else ShieldEmeraldSafe
                )
                MetricCard(
                    title = "High-Risk Users",
                    value = "${metrics?.highRiskUsers ?: 0}",
                    modifier = Modifier.weight(1f),
                    color = if ((metrics?.highRiskUsers ?: 0) > 0) ShieldRoseRisk else ShieldEmeraldSafe
                )
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun AdminUsersTab(
    users: List<UserEntity>,
    onUpdateStatus: (String, String) -> Unit,
    onAdjustBalance: (String, Long, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForAdjustment by remember { mutableStateOf<UserEntity?>(null) }
    var adjustAmountText by remember { mutableStateOf("") }
    var adjustReasonText by remember { mutableStateOf("") }

    val filtered = users.filter {
        it.username.contains(searchQuery, true) || it.email.contains(searchQuery, true)
    }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users by username or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { user ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(user.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusBadge(status = user.status)
                                RiskBadge(score = user.riskScore)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Balance: ${user.coins} Coins", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ShieldGoldPrimary)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Admin Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (user.status == "ACTIVE") {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(user.id, "SAFE_MODE") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Safe Mode", fontSize = 10.sp)
                                }
                                OutlinedButton(
                                    onClick = { onUpdateStatus(user.id, "SUSPENDED") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Suspend", fontSize = 10.sp, color = Color(0xFFF59E0B))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(user.id, "ACTIVE") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Activate", fontSize = 10.sp, color = ShieldEmeraldSafe)
                                }
                            }

                            Button(
                                onClick = { selectedUserForAdjustment = user },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text("Adjust +/-", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }

    // Balance Adjustment Dialog
    selectedUserForAdjustment?.let { target ->
        Dialog(onDismissRequest = { selectedUserForAdjustment = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, ShieldGoldPrimary, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Administrative Balance Adjustment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Target: ${target.username} (Current: ${target.coins} coins)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = adjustAmountText,
                        onValueChange = { adjustAmountText = it },
                        label = { Text("Coin Delta (+50 or -50)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adjustReasonText,
                        onValueChange = { adjustReasonText = it },
                        label = { Text("Mandatory Audit Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { selectedUserForAdjustment = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val delta = adjustAmountText.toLongOrNull() ?: 0L
                                if (delta != 0L && adjustReasonText.isNotBlank()) {
                                    onAdjustBalance(target.id, delta, adjustReasonText)
                                    selectedUserForAdjustment = null
                                    adjustAmountText = ""
                                    adjustReasonText = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary)
                        ) {
                            Text("Apply & Log", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRewardsTab(
    config: RewardConfigEntity?,
    onSaveConfig: (Long, Long, Long, Boolean) -> Unit
) {
    var rewardAmountText by remember(config) { mutableStateOf(config?.rewardAmount?.toString() ?: "10") }
    var dailyLimitText by remember(config) { mutableStateOf(config?.dailyLimit?.toString() ?: "100") }
    var cooldownText by remember(config) { mutableStateOf(config?.cooldownSeconds?.toString() ?: "15") }
    var isEnabled by remember(config) { mutableStateOf(config?.isWatchAndEarnEnabled ?: true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Reward Engine Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "All modifications generate append-only audit entries recording exact parameter changes.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Watch & Earn Feature Active", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = ShieldGoldPrimary)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = rewardAmountText,
                onValueChange = { rewardAmountText = it.filter { c -> c.isDigit() } },
                label = { Text("Reward Amount per Ad (Coins)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = dailyLimitText,
                onValueChange = { dailyLimitText = it.filter { c -> c.isDigit() } },
                label = { Text("Daily Earning Limit per User (Coins)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = cooldownText,
                onValueChange = { cooldownText = it.filter { c -> c.isDigit() } },
                label = { Text("Minimum Cooldown Interval (Seconds)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amount = rewardAmountText.toLongOrNull() ?: 10L
                    val limit = dailyLimitText.toLongOrNull() ?: 100L
                    val cool = cooldownText.toLongOrNull() ?: 15L
                    onSaveConfig(amount, limit, cool, isEnabled)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_reward_config_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration & Write Audit Log", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun AdminWithdrawalsTab(
    withdrawals: List<WithdrawalEntity>,
    onReview: (String, Boolean, String) -> Unit
) {
    var reviewingWithdrawal by remember { mutableStateOf<WithdrawalEntity?>(null) }
    var reviewNote by remember { mutableStateOf("") }
    var isApproving by remember { mutableStateOf(true) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (withdrawals.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No withdrawal requests in database.", fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            items(withdrawals) { w ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${w.amount} Coins • ${w.paymentMethod}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(w.status, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ShieldGoldPrimary)
                        }

                        Text("User: ${w.userId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Target: ${w.paymentDetails}", fontSize = 12.sp)

                        w.adminNote?.let {
                            Text("Note: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (w.status == "PENDING") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        reviewingWithdrawal = w
                                        isApproving = true
                                        reviewNote = "Approved standard payout"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ShieldEmeraldSafe)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        reviewingWithdrawal = w
                                        isApproving = false
                                        reviewNote = "Account or information verification issue"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldRoseRisk)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject & Refund", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(70.dp)) }
    }

    reviewingWithdrawal?.let { target ->
        Dialog(onDismissRequest = { reviewingWithdrawal = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, ShieldGoldPrimary, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        if (isApproving) "Confirm Withdrawal Approval" else "Confirm Rejection & Refund",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${target.amount} coins via ${target.paymentMethod} to ${target.paymentDetails}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it },
                        label = { Text("Audit Note / Justification") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { reviewingWithdrawal = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onReview(target.id, isApproving, reviewNote)
                                reviewingWithdrawal = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isApproving) ShieldEmeraldSafe else ShieldRoseRisk
                            )
                        ) {
                            Text(if (isApproving) "Approve" else "Reject & Refund", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSecurityTab(
    riskEvents: List<RiskEventEntity>,
    highRiskUsers: List<UserEntity>,
    onUpdateStatus: (String, String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("High-Risk & Safe Mode Accounts (${highRiskUsers.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (highRiskUsers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No high-risk users flagged at this time.", fontSize = 11.sp, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(highRiskUsers) { u ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShieldRoseRisk.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(u.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Score: ${u.riskScore}/100 • Status: ${u.status}", fontSize = 11.sp, color = ShieldRoseRisk)
                        }
                        if (u.status == "SAFE_MODE") {
                            Button(
                                onClick = { onUpdateStatus(u.id, "ACTIVE") },
                                colors = ButtonDefaults.buttonColors(containerColor = ShieldEmeraldSafe),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Disable Safe Mode", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Fraud Risk Events Log (${riskEvents.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(riskEvents) { ev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ev.reason, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(ev.severity, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ShieldRoseRisk)
                    }
                    Text(
                        "User: ${ev.userId.take(8)}... (Score: ${ev.previousScore} -> ${ev.newScore}) • ${dateFormat.format(Date(ev.createdAt))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

@Composable
fun AdminAuditLogsTab(
    logs: List<AuditLogEntity>,
    dateFormat: SimpleDateFormat
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "Append-Only Audit Trail (${logs.size} Records)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "Cryptographically preserved records of every critical action. Immutable.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(logs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ShieldGoldPrimary)
                        Text(dateFormat.format(Date(log.createdAt)), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Actor: ${log.actorUsername} (${log.actorId.take(8)}...) -> ${log.targetType}:${log.targetId.take(8)}...",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        log.metadata,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}
