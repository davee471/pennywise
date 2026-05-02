package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType

@Composable
fun StatsView(
    txController: TransactionController,
    cycleId: Long
) {
    var totalSpent by remember { mutableStateOf(0.0) }
    var pieChartData by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    // Fetch and calculate stats whenever the view opens
    LaunchedEffect(cycleId) {
        val res = txController.getHistory(cycleId)
        val transactions = if (res is stud.brokers.pennywise.util.Result.Success<*>) {
            @Suppress("UNCHECKED_CAST")
            res.data as List<Transaction>
        } else emptyList()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }

        // 1. Calculate Total Spent
        totalSpent = expenses.sumOf { it.amount }

        // 2. Group by category for the Pie Chart
        pieChartData = expenses.groupBy { it.category.name }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Circular "Total Spent" Display (similar to Dashboard limit)
        Card(
            modifier = Modifier
                .size(220.dp)
                .padding(16.dp),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Total Spent", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$totalSpent EGP",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error // Red to indicate spent budget
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Reusing the exact same chart component from DashboardView.kt!
        RenderPieChart(data = pieChartData)
    }
}