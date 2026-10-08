package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CoinTransactionEntity
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.RiskBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.SystemNoticeBanner
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldGoldDark
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.theme.ShieldRoseRisk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    user: UserEntity,
    config: RewardConfigEntity?,
    todayEarned: Long,
    recentTransactions: List<CoinTransactionEntity>,
    onNavigateToEarn: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyLimit = config?.dailyLimit ?: 100L
    val remainingDaily = (dailyLimit - todayEarned).coerceAtLeast(0L)
    val progress = (todayEarned.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)
    val isKillSwitch = config?.isKillSwitchActive == true

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // User Identity & Security Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${user.username}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ID: ${user.id.take(8)}...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(status = user.status)
                    RiskBadge(score = user.riskScore)
                }
            }
        }

        // System Notices (Kill Switch, Safe Mode, Suspended)
        item {
            SystemNoticeBanner(isKillSwitchActive = isKillSwitch, userStatus = user.status)
        }

        // Hero Balance Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(ShieldGoldPrimary, ShieldGoldDark)),
                        RoundedCornerShape(22.dp)
                    )
                    .background(
                        Brush.verticalGradient(listOf(DarkSurfaceElevated, DarkSurface)),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .testTag("balance_hero_card")
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL AVAILABLE BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = ShieldGoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${user.coins}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = ShieldGoldPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COINS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = "≈ $${String.format(Locale.US, "%.2f", user.coins * 0.01)} USD / ৳${String.format(Locale.US, "%.1f", user.coins * 1.2)} BDT",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily earning progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Today's Earnings",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$todayEarned / $dailyLimit coins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShieldGoldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ShieldGoldPrimary,
                        trackColor = Color(0xFF283655)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Remaining today: $remainingDaily coins",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToEarn,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("action_earn_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary),
                    enabled = !isKillSwitch && user.status == "ACTIVE"
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Watch & Earn", fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Button(
                    onClick = onNavigateToWithdraw,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("action_withdraw_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    enabled = !isKillSwitch && user.status == "ACTIVE"
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Withdraw", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.clickable { onNavigateToHistory() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ShieldGoldPrimary
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = ShieldGoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No recent transactions. Watch rewarded ads to earn your first coins!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(recentTransactions.take(5)) { tx ->
                TransactionRow(tx = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // padding for bottom nav
        }
    }
}

@Composable
fun TransactionRow(tx: CoinTransactionEntity, modifier: Modifier = Modifier) {
    val isCredit = tx.amount > 0
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCredit) ShieldEmeraldSafe.copy(alpha = 0.15f)
                            else ShieldRoseRisk.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) ShieldEmeraldSafe else ShieldRoseRisk,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(tx.createdAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isCredit) "+${tx.amount}" else "${tx.amount}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) ShieldEmeraldSafe else ShieldRoseRisk
                )
                Text(
                    text = "Bal: ${tx.balanceAfter}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
