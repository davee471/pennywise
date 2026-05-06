package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.models.Category
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.models.TransactionType

/**
 * TransactionView is a Composable screen that allows users to log new expenses
 * or edit existing ones. It includes fields for amount and category selection.
 * 
 * @param txController The controller handling transaction logic and database interactions.
 * @param cycleId The ID of the current budget cycle this transaction belongs to.
 * @param currencySymbol The user's preferred currency symbol to display in the UI.
 * @param transactionToEdit An optional existing transaction to pre-fill the form for editing.
 * @param onTransactionSaved Callback invoked after a transaction is successfully saved.
 * @param onCancel Callback invoked when the user cancels the action.
 */
@Composable
fun TransactionView(
    txController: TransactionController,
    budgetController: BudgetController,
    cycleId: Long,
    currencySymbol: String,
    transactionToEdit: Transaction? = null,
    onTransactionSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // State for the transaction amount, initialized with existing value if editing.
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    
    // State for the selected category.
    var selectedCategory by remember { mutableStateOf<Category?>(transactionToEdit?.category) }
    
    // State to trigger category refresh after adding a new one
    var categoryRefreshKey by remember { mutableStateOf(0) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    // Fetch available categories from the database.
    val categories by produceState<List<Category>>(initialValue = emptyList(), categoryRefreshKey) {
        value = txController.getCategories().getOrNull() ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Bar with Cancel button, Title, and Save button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
            }
            Text(
                text = if (transactionToEdit == null) "New Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(
                onClick = {
                    val valAmount = amount.toDoubleOrNull() ?: 0.0
                    val category = selectedCategory
                    if (valAmount > 0 && category != null) {
                        scope.launch {
                            // Determine whether to log a new expense or update an existing one
                            if (transactionToEdit == null) {
                                budgetController.logExpense(valAmount, category)
                            }
                            else {
                                txController.editTransaction(transactionToEdit.id, valAmount, category)
                                if(transactionToEdit.type == TransactionType.INCOME){
                                    budgetController.editIncome(valAmount - transactionToEdit.amount)
                                }
                            }
                            onTransactionSaved()
                        }
                    }
                },
                // The Save button is only enabled if a valid amount and category are selected
                enabled = amount.toDoubleOrNull() != null && selectedCategory != null
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input Field
        Text("Amount", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = amount,
            onValueChange = { 
                // Only allow digits and a single decimal point.
                if (it.all { char -> char.isDigit() || char == '.' }) amount = it 
            },
            placeholder = { Text("0.00") },
            suffix = { Text(currencySymbol) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center, 
                color = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Category Selection Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Select Category", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showAddCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of category chips for quick selection.
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory?.id == category.id,
                    onClick = { selectedCategory = category },
                    label = { 
                        Text(
                            text = category.name,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Custom Category Dialog
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add Custom Category") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            scope.launch {
                                // Pass a generic default icon like "label" for custom categories
                                txController.addCategory(newCategoryName.trim(), "label")
                                categoryRefreshKey++ // Trigger categories reload
                                showAddCategoryDialog = false
                                newCategoryName = "" // Reset input
                            }
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}




// Private helper to safely extract Result data without touching core files
private fun <T> stud.brokers.pennywise.util.Result<T>.getOrNull(): T? {
    return when (this) {
        is stud.brokers.pennywise.util.Result.Success<*> -> {
            @Suppress("UNCHECKED_CAST")
            this.data as T
        }
        is stud.brokers.pennywise.util.Result.Error -> null
    }
}
