package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    isPinEnabled: Boolean,
    isNotificationsEnabled: Boolean,
    isDarkTheme: Boolean,
    currencySymbol: String,
    onExportPdfClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onTogglePinClick: (Boolean) -> Unit,
    onToggleNotificationsClick: (Boolean) -> Unit,
    onToggleThemeClick: (Boolean) -> Unit,
    onChangeCurrencyClick: (String) -> Unit,
    onResetCycleClick: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SettingsCard(title = "Preferences") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onToggleThemeClick
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Currency Symbol", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { showCurrencyDialog = true }) {
                    Text(currencySymbol, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily Notifications", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = onToggleNotificationsClick
                )
            }
        }

        SettingsCard(title = "Data & Backup") {
            Button(
                onClick = onExportPdfClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export")
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORT INVOICE TO PDF")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onExportBackupClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("BACKUP DATA TO JSON")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("RESTORE DATA FROM JSON")
            }
        }

        SettingsCard(title = "Security") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("App PIN Lock", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isPinEnabled,
                    onCheckedChange = onTogglePinClick
                )
            }
        }

        SettingsCard(title = "Full Reset", borderColor = MaterialTheme.colorScheme.error) {
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Cycle & Clear Logs")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Cycle?") },
            text = { Text("This will permanently delete all logs and clear the active cycle. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetCycleClick()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore from Backup?") },
            text = { Text("This will overwrite all current data with the contents of 'pennywise_backup.json'. Ensure the file is in your Downloads/Documents folder. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImportBackupClick()
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCurrencyDialog) {
        var newCurrency by remember { mutableStateOf(currencySymbol) }
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Change Currency") },
            text = {
                OutlinedTextField(
                    value = newCurrency,
                    onValueChange = { if(it.length <= 3) newCurrency = it },
                    label = { Text("Symbol (e.g. EGP, $, €)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onChangeCurrencyClick(newCurrency)
                    showCurrencyDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    borderColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}