package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.models.Category

@Composable
fun TransactionView(
    txController: TransactionController,
    cycleId: Long,
    onTransactionSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    val categories by produceState<List<Category>>(initialValue = emptyList()) {
        value = txController.getCategories().getOrNull() ?: emptyList()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("New Expense", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
            label = { Text("Amount") },
            suffix = { Text("EGP") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select Category", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val valAmount = amount.toDoubleOrNull() ?: 0.0
                            if (valAmount > 0) {
                                scope.launch {
                                    txController.logExpense(valAmount, category, cycleId)
                                    onTransactionSaved()
                                }
                            }
                        }
                ) {
                    Text(
                        text = category.name,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
