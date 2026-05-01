package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType

@Composable
fun HistoryView(txController: TransactionController, cycleId: Long) {
    // Fetch transactions for the current cycle
    val transactions by produceState<List<Transaction>>(initialValue = emptyList(), key1 = cycleId) {
        value = txController.getHistory(cycleId).getOrNull() ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "History",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No Transactions Found",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn {
                items(transactions) { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = tx.category.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = tx.date.toString(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // Format amount with + or - based on type
                        Text(
                            text = "${if (tx.type == TransactionType.INCOME) "+" else "-"}${tx.amount} EGP",
                            color = if (tx.type == TransactionType.INCOME) Color.Green else Color.Red
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
