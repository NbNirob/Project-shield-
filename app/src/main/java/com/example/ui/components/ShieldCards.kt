package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.security.RiskLevel
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.theme.ShieldRoseRisk

@Composable
fun RiskBadge(score: Int, modifier: Modifier = Modifier) {
    val (color, label) = when {
        score < 30 -> ShieldEmeraldSafe to "LOW ($score)"
        score < 60 -> Color(0xFFF59E0B) to "MEDIUM ($score)"
        score < 80 -> Color(0xFFF97316) to "HIGH ($score)"
        else -> ShieldRoseRisk to "CRITICAL ($score)"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (color, icon) = when (status) {
        "ACTIVE" -> ShieldEmeraldSafe to Icons.Default.CheckCircle
        "SUSPENDED" -> Color(0xFFF59E0B) to Icons.Default.Warning
        "BANNED" -> ShieldRoseRisk to Icons.Default.Block
        "SAFE_MODE" -> Color(0xFF8B5CF6) to Icons.Default.Lock
        else -> Color.Gray to Icons.Default.Security
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = status, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SystemNoticeBanner(
    isKillSwitchActive: Boolean,
    userStatus: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isKillSwitchActive || userStatus != "ACTIVE") {
        Column(modifier = modifier.fillMaxWidth()) {
            if (isKillSwitchActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShieldRoseRisk.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, ShieldRoseRisk, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ShieldRoseRisk)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "GLOBAL KILL SWITCH ACTIVE",
                                color = ShieldRoseRisk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                "Reward claiming and withdrawals are temporarily halted by administration for maintenance.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (userStatus == "SAFE_MODE") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "ACCOUNT IN SAFE MODE",
                                color = Color(0xFF8B5CF6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                "High risk detected. Earning & withdrawals are locked. Contact admin to review.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else if (userStatus == "SUSPENDED" || userStatus == "BANNED") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShieldRoseRisk.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, ShieldRoseRisk, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = ShieldRoseRisk)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "ACCOUNT $userStatus",
                                color = ShieldRoseRisk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                "Earning privileges have been revoked by compliance administrators.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardedAdSimulationDialog(
    isPlaying: Boolean,
    progressSeconds: Int,
    targetSeconds: Int = 6,
    onCancel: () -> Unit
) {
    if (isPlaying) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, ShieldGoldPrimary, RoundedCornerShape(20.dp))
                    .testTag("ad_simulation_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ShieldGoldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "SPONSORED REWARD AD",
                                color = ShieldGoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${targetSeconds - progressSeconds}s remaining",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(ShieldGoldPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ShieldGoldPrimary,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Project Shield Partner Showcase",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Simulating Google AdMob Rewarded Video stream. Complete playback to verify reward cryptographic token.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    val progress = (progressSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ShieldGoldPrimary,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().testTag("cancel_ad_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldRoseRisk)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Close Ad (Forfeit Reward)")
                    }
                }
            }
        }
    }
}
