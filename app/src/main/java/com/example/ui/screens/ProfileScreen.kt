package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.local.entity.UserEntity
import com.example.security.RiskEngine
import com.example.security.RiskLevel
import com.example.ui.components.RiskBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.theme.ShieldRoseRisk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    user: UserEntity,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val riskLevel = RiskEngine.getRiskLevel(user.riskScore)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User Profile & Security",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Profile Avatar Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = ShieldGoldPrimary.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = ShieldGoldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user.username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(status = user.status)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ROLE: ${user.role}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Security Risk Score Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ShieldGoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fraud Risk Score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    RiskBadge(score = user.riskScore)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val riskProgress = (user.riskScore.toFloat() / 100f).coerceIn(0f, 1f)
                val barColor = when (riskLevel) {
                    RiskLevel.LOW -> ShieldEmeraldSafe
                    RiskLevel.MEDIUM -> Color(0xFFF59E0B)
                    RiskLevel.HIGH -> Color(0xFFF97316)
                    RiskLevel.CRITICAL -> ShieldRoseRisk
                }

                LinearProgressIndicator(
                    progress = { riskProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Risk Level: ${riskLevel.name} (${user.riskScore}/100)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor
                )

                Text(
                    text = "Calculated server-side based on duplicate attempts, timestamp anomalies, rapid queries, and authentication signals. Scores >= 80 auto-trigger Safe Mode.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Account Details Breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileDetailRow(label = "User UUID", value = user.id)
                ProfileDetailRow(label = "Current Balance", value = "${user.coins} Coins")
                ProfileDetailRow(label = "Member Since", value = dateFormat.format(Date(user.createdAt)))
                ProfileDetailRow(label = "Last Active", value = dateFormat.format(Date(user.lastLoginAt)))
            }
        }

        // Admin Portal Entry Button (for Admin users or rapid inspection)
        if (user.role == "ADMIN") {
            Button(
                onClick = onNavigateToAdmin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("open_admin_portal_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Protected Admin Dashboard", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("logout_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldRoseRisk)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ShieldRoseRisk)
            Spacer(modifier = Modifier.width(8.dp))
            Text("End Secure Session (Logout)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
