package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import stud.brokers.pennywise.models.Category


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    categories: List<Category>,
    isPinEnabled: Boolean,
    isNotificationsEnabled: Boolean,
    currencySymbol: String,
    onAddCategory: (name: String, iconName: String) -> Unit,
    onExportCsvClick: () -> Unit,
    onTogglePinClick: (Boolean) -> Unit,
    onToggleNotificationsClick: (Boolean) -> Unit,
    onChangeCurrencyClick: (String) -> Unit,
    onResetCycleClick: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

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

        SettingsCard(title = "Categories") {
            categories.forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = getIconByName(category.iconName), contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { showCategoryDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD CATEGORY")
            }
        }

        SettingsCard(title = "Data & Backup") {
            Button(
                onClick = onExportCsvClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export")
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORT TO CSV")
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

        SettingsCard(title = "Danger Zone", borderColor = MaterialTheme.colorScheme.error) {
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

    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onConfirm = { name, icon ->
                onAddCategory(name, icon)
                showCategoryDialog = false
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

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("home") }
    val availableIcons = listOf("home", "shopping_cart", "fastfood", "directions_car", "local_hospital", "sports_esports")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
                Text("Select Icon:", style = MaterialTheme.typography.bodyMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(availableIcons) { iconName ->
                        IconButton(onClick = { selectedIcon = iconName }) {
                            Icon(
                                imageVector = getIconByName(iconName),
                                contentDescription = iconName,
                                tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(categoryName, selectedIcon) },
                enabled = categoryName.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun getIconByName(name: String): ImageVector {
    return when (name.lowercase()) {
        "shopping_cart" -> Icons.Default.ShoppingCart
        "fastfood", "food" -> Icons.Default.ShoppingCart
        "directions_car", "transport" -> Icons.Default.Place
        "local_hospital" -> Icons.Default.Add
        "sports_esports", "gaming" -> Icons.Default.PlayArrow
        else -> Icons.Default.Home
    }
}

@Preview
@Composable
fun SettingsViewPreview() {
    MaterialTheme {
        SettingsView(
            categories = listOf(
                Category(id = 1, name = "Groceries", iconName = "shopping_cart"),
                Category(id = 2, name = "Transport", iconName = "directions_car")
            ),
            isPinEnabled = true,
            isNotificationsEnabled = false,
            currencySymbol = "EGP",
            onAddCategory = { _, _ -> },
            onExportCsvClick = { },
            onTogglePinClick = { },
            onToggleNotificationsClick = { },
            onChangeCurrencyClick = { },
            onResetCycleClick = { }
        )
    }
}