package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryView(
    txController: TransactionController,
    cycleId: Long,
    onEditTransaction: (Transaction) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    
    // Load data when filters change
    LaunchedEffect(cycleId, selectedCategory) {
        transactions = txController.getHistory(cycleId, selectedCategory).getOrNull() ?: emptyList()
        categories = txController.getCategories().getOrNull() ?: emptyList()
    }

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // Delete Confirmation Dialog (US #8)
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    transactionToDelete?.let { tx ->
                        scope.launch {
                            txController.deleteTransaction(tx.id)
                            // Refresh list
                            transactions = txController.getHistory(cycleId, selectedCategory).getOrNull() ?: emptyList()
                        }
                    }
                    transactionToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Category chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory?.id == category.id,
                    onClick = { selectedCategory = category },
                    label = { Text(category.name) }
                )
            }
        }

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results for filter", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Sort by timestamp descending so newest are on top
            val sortedTransactions = transactions.sortedByDescending { it.timestamp }
            val grouped = sortedTransactions.groupBy { it.date }
            
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val yesterday = today.minus(1, DateTimeUnit.DAY)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (date, txList) ->
                    item {
                        val header = when (date) {
                            today -> "Today"
                            yesterday -> "Yesterday"
                            else -> {
                                // Simple format: 12 Oct 2023
                                val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
                                "${date.dayOfMonth} $month ${date.year}"
                            }
                        }
                        Text(
                            text = header,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp, 8.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(txList) { tx ->
                        TransactionItem(
                            tx = tx,
                            onEdit = { onEditTransaction(tx) },
                            onDelete = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    tx: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMenu = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Amount display
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    val color = if (tx.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Text(
                        text = "${if (tx.type == TransactionType.INCOME) "+" else "-"}${tx.amount.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = tx.category.name, style = MaterialTheme.typography.bodyLarge)
            }
            
            // Menu for Edit/Delete on click
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { 
                        showMenu = false
                        onEdit() 
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { 
                        showMenu = false
                        onDelete() 
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
