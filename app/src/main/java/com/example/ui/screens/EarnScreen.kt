package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RewardConfigEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.SystemNoticeBanner
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.theme.ShieldRoseRisk

@Composable
fun EarnScreen(
    user: UserEntity,
    config: RewardConfigEntity?,
    todayEarned: Long,
    onStartWatchAd: () -> Unit,
    onTestReplayAttack: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val rewardAmount = config?.rewardAmount ?: 10L
    val dailyLimit = config?.dailyLimit ?: 100L
    val cooldown = config?.cooldownSeconds ?: 15L
    val isEnabled = config?.isWatchAndEarnEnabled == true
    val isKillSwitch = config?.isKillSwitchActive == true
    val remainingToday = (dailyLimit - todayEarned).coerceAtLeast(0L)

    var replayKeyInput by remember { mutableStateOf("REPLAY_TEST_KEY_999") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Notice Banner
        SystemNoticeBanner(isKillSwitchActive = isKillSwitch, userStatus = user.status)

        // Hero Earn Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ShieldGoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(ShieldGoldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ShieldGoldPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Watch Rewarded Ad",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Earn +$rewardAmount coins per verified completion",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ShieldGoldPrimary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Parameters Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ParameterItem(
                        icon = Icons.Default.Security,
                        label = "Reward",
                        value = "+$rewardAmount Coins"
                    )
                    ParameterItem(
                        icon = Icons.Default.Timer,
                        label = "Cooldown",
                        value = "${cooldown}s"
                    )
                    ParameterItem(
                        icon = Icons.Default.Info,
                        label = "Daily Cap",
                        value = "$todayEarned / $dailyLimit"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val canEarn = isEnabled && !isKillSwitch && user.status == "ACTIVE" && remainingToday >= rewardAmount

                Button(
                    onClick = onStartWatchAd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("watch_ad_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary),
                    enabled = canEarn
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!isEnabled) "Watch & Earn Disabled"
                        else if (isKillSwitch) "Kill Switch Active"
                        else if (user.status != "ACTIVE") "Account Restricted"
                        else if (remainingToday < rewardAmount) "Daily Cap Reached"
                        else "Watch Ad (+${rewardAmount} Coins)",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Anti-Fraud & Replay Attack Demo Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = ShieldGoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ANTI-REPLAY ATTACK LAB",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShieldGoldPrimary
                    )
                }

                Text(
                    text = "Project Shield enforces unique idempotency constraints in the database. Test submitting the same reward key twice to witness duplicate rejection and risk scoring.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                )

                OutlinedButton(
                    onClick = { onTestReplayAttack(replayKeyInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_replay_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Trigger Replay with Key: $replayKeyInput", fontSize = 11.sp)
                }
            }
        }

        // Verification Architecture Explanation
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ShieldEmeraldSafe)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AdMob Verification Pipeline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "• Active Engine: DevelopmentRewardVerifier\n" +
                            "• Verifies cryptographic proof token, user binding, and playback duration.\n" +
                            "• Production Ready: AdMobRewardVerifier abstraction is structured to handle Google ECDSA public key signature verification over query callbacks.\n" +
                            "• Zero-Trust: Backend computes coin grants and daily quotas independently.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun ParameterItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = ShieldGoldPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
