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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CoinTransactionEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.ui.theme.ShieldGoldPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    transactions: List<CoinTransactionEntity>,
    withdrawals: List<WithdrawalEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Transactions, 1: Activity Timeline
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "History & Audit Logs",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Coin Ledger", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Activity Timeline", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            if (transactions.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No coin transactions recorded yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(transactions) { tx ->
                        TransactionRow(tx = tx)
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        } else {
            // User-friendly Activity Timeline
            val timelineEvents = remember(transactions, withdrawals) {
                val list = mutableListOf<TimelineItem>()
                transactions.forEach { tx ->
                    list.add(
                        TimelineItem(
                            title = if (tx.amount > 0) "Reward Credited" else "Coins Reserved",
                            description = tx.description,
                            timestamp = tx.createdAt,
                            badge = if (tx.amount > 0) "+${tx.amount} coins" else "${tx.amount} coins",
                            badgeColor = if (tx.amount > 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                        )
                    )
                }
                withdrawals.forEach { w ->
                    list.add(
                        TimelineItem(
                            title = "Withdrawal Update",
                            description = "Payout ${w.amount} coins via ${w.paymentMethod} - Status: ${w.status}" +
                                    (w.adminNote?.let { " ($it)" } ?: ""),
                            timestamp = w.updatedAt,
                            badge = w.status,
                            badgeColor = if (w.status == "COMPLETED") Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    )
                }
                list.sortedByDescending { it.timestamp }
            }

            if (timelineEvents.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No activity events recorded yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(timelineEvents) { ev ->
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
                                    Text(
                                        text = ev.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = ev.badge,
                                        color = ev.badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ev.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormat.format(Date(ev.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }
}

data class TimelineItem(
    val title: String,
    val description: String,
    val timestamp: Long,
    val badge: String,
    val badgeColor: Color
)
