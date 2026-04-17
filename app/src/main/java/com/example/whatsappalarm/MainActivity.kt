package com.example.whatsappalarm

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatsappalarm.ui.theme.WhatsAppAlarmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = WatchedContactsPrefs(this)

        setContent {
            WhatsAppAlarmTheme {
                val contacts by prefs.contactsFlow.collectAsStateWithLifecycle(initialValue = prefs.getContacts())
                var showDialog by remember { mutableStateOf(false) }
                var newName by remember { mutableStateOf("") }
                val permissionGranted = isNotificationListenerEnabled()

                MainScreen(
                    permissionGranted = permissionGranted,
                    contacts = contacts.toList(),
                    onRequestPermission = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onAddContact = { showDialog = true },
                    onRemoveContact = { prefs.removeContact(it) }
                )

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false; newName = "" },
                        title = { Text("Add Contact Name") },
                        text = {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Name (as saved in contacts)") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newName.isNotBlank()) {
                                    prefs.addContact(newName.trim())
                                    newName = ""
                                    showDialog = false
                                }
                            }) { Text("Add") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false; newName = "" }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val cn = ComponentName(this, WhatsAppNotificationListener::class.java)
        return flat.split(":").any {
            try { ComponentName.unflattenFromString(it) == cn } catch (e: Exception) { false }
        }
    }
}

@Composable
fun MainScreen(
    permissionGranted: Boolean,
    contacts: List<String>,
    onRequestPermission: () -> Unit,
    onAddContact: () -> Unit,
    onRemoveContact: (String) -> Unit
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0A0A), Color(0xFF1A1A2E))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Text(
                text = "WA Alarm",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF25D366),
                letterSpacing = (-1).sp
            )
            Text(
                text = "Wake up when they message you",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Permission Card
            PermissionCard(granted = permissionGranted, onRequest = onRequestPermission)

            Spacer(modifier = Modifier.height(24.dp))

            // Contacts section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Watched Contacts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(
                    onClick = onAddContact,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF25D366))
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No contacts added yet.\nTap + to add someone.",
                        color = Color(0xFF555555),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(contacts) { contact ->
                        ContactRow(name = contact, onRemove = { onRemoveContact(contact) })
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(granted: Boolean, onRequest: () -> Unit) {
    val cardColor = if (granted) Color(0xFF0D2B1A) else Color(0xFF2B0D0D)
    val accentColor = if (granted) Color(0xFF25D366) else Color(0xFFFF4444)
    val statusText = if (granted) "✓  Notification Access Granted" else "✗  Notification Access Required"
    val subText = if (granted)
        "App is actively watching for messages"
    else
        "Tap below to grant notification listener permission"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .padding(20.dp)
    ) {
        Text(statusText, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(subText, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        if (!granted) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContactRow(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1E1E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF25D366).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.first().uppercase(),
                    color = Color(0xFF25D366),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFF555555))
        }
    }
}