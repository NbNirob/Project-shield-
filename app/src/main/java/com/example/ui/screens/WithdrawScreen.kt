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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.theme.ShieldRoseRisk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WithdrawScreen(
    user: UserEntity,
    withdrawals: List<WithdrawalEntity>,
    onSubmitWithdrawal: (Long, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val paymentMethods = listOf("bKash", "Nagad", "Rocket", "Bank Transfer", "PayPal", "Crypto (USDT)")
    var selectedMethod by remember { mutableStateOf(paymentMethods[0]) }
    var amountText by remember { mutableStateOf("") }
    var accountDetails by remember { mutableStateOf("") }

    val amountLong = amountText.toLongOrNull() ?: 0L
    val canSubmit = amountLong >= 20 && amountLong <= user.coins && accountDetails.trim().length >= 5 && user.status == "ACTIVE"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE FOR WITHDRAWAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${user.coins} Coins",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = ShieldGoldPrimary
                        )
                    }

                    Surface(
                        color = ShieldGoldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Min: 20 Coins",
                            color = ShieldGoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "1. Select Payout Method",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Method chips in rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentMethods.take(3).forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShieldGoldPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentMethods.drop(3).forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShieldGoldPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "2. Payment Account / Wallet Address",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = accountDetails,
                        onValueChange = { accountDetails = it },
                        placeholder = { Text(if (selectedMethod.contains("bKash") || selectedMethod.contains("Nagad")) "e.g. 01700000000" else "Account number or wallet address") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_account_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "3. Withdrawal Amount (Coins)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("Minimum 20 coins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_amount_input")
                    )

                    // Quick percentage chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 50, 100).forEach { pct ->
                            val pctAmount = (user.coins * pct / 100).coerceAtLeast(0L)
                            OutlinedButton(
                                onClick = { amountText = pctAmount.toString() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$pct%", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onSubmitWithdrawal(amountLong, selectedMethod, accountDetails)
                            amountText = ""
                            accountDetails = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_withdrawal_button"),
                        enabled = canSubmit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary)
                    ) {
                        Text(
                            text = if (amountLong > user.coins) "Insufficient Balance"
                            else if (amountLong < 20) "Minimum 20 Coins Required"
                            else "Submit Withdrawal Request",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Withdrawal History Title
        item {
            Text(
                "Your Withdrawal Requests",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (withdrawals.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No withdrawal requests yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(withdrawals) { w ->
                WithdrawalItemCard(withdrawal = w)
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
fun WithdrawalItemCard(withdrawal: WithdrawalEntity, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    val (statusColor, statusIcon) = when (withdrawal.status) {
        "PENDING" -> Color(0xFFF59E0B) to Icons.Default.HourglassEmpty
        "COMPLETED", "APPROVED" -> ShieldEmeraldSafe to Icons.Default.CheckCircle
        "REJECTED" -> ShieldRoseRisk to Icons.Default.HourglassEmpty
        else -> Color.Gray to Icons.Default.HourglassEmpty
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${withdrawal.amount} Coins (${withdrawal.paymentMethod})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = withdrawal.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Target: ${withdrawal.paymentDetails}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            withdrawal.adminNote?.let { note ->
                Text(
                    text = "Admin Note: $note",
                    fontSize = 11.sp,
                    color = if (withdrawal.status == "REJECTED") ShieldRoseRisk else ShieldEmeraldSafe,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = dateFormat.format(Date(withdrawal.createdAt)),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
