package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldGoldPrimary
import com.example.ui.theme.ShieldRoseRisk

@Composable
fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onQuickSwitch: (String) -> Unit,
    statusMessage: String?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Crest
        Surface(
            color = ShieldGoldPrimary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .size(76.dp)
                .border(1.5.dp, ShieldGoldPrimary, RoundedCornerShape(20.dp))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = ShieldGoldPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PROJECT SHIELD",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Secure Rewarded Platform & Anti-Fraud Engine",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Status Message Banner if any
        statusMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.contains("failed", true) || msg.contains("error", true))
                        ShieldRoseRisk.copy(alpha = 0.15f)
                    else ShieldEmeraldSafe.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Quick Demo Accounts Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "INSTANT DEMO ACCESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShieldGoldPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onQuickSwitch("admin") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("demo_admin_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Admin Portal", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onQuickSwitch("shielduser") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("demo_user_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Regular User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Auth Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Sign In", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_login")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Create Account", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_register")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Fields
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(if (selectedTab == 0) "Username or Email" else "Username") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input")
        )

        if (selectedTab == 1) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (selectedTab == 0) {
                    onLogin(username, password)
                } else {
                    onRegister(username, email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("auth_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ShieldGoldPrimary)
        ) {
            Text(
                text = if (selectedTab == 0) "Authenticate Session" else "Register Protected Account",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}
