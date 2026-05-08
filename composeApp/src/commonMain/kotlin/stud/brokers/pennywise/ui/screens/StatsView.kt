package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import stud.brokers.pennywise.controllers.TransactionController
import stud.brokers.pennywise.controllers.BudgetController
import stud.brokers.pennywise.models.Transaction
import stud.brokers.pennywise.models.TransactionType

/**
 * A screen providing a statistical overview of the current budget cycle.
 * Displays available allowance, total spent, and categorical spending distribution.
 *
 * @param txController The controller for transaction-related operations.
 * @param budgetController The controller for budget-related operations.
 * @param cycleId The ID of the budget cycle to display statistics for.
 * @param currencySymbol The currency symbol used for displaying amounts.
 */
@Composable
fun StatsView(
    txController: TransactionController,
    budgetController: BudgetController,
    cycleId: Long,
    currencySymbol: String
) {
    var totalSpent by remember { mutableStateOf(0.0) }
    var totalAllowance by remember { mutableStateOf(0.0) }
    var pieChartData by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    // Fetch and calculate stats whenever the cycle ID or active cycle state changes
    LaunchedEffect(cycleId, budgetController.activeCycle) {
        totalAllowance = budgetController.activeCycle?.totalAllowance ?: 0.0
        
        val res = txController.getHistory(cycleId)
        val transactions = if (res is stud.brokers.pennywise.util.Result.Success<*>) {
            @Suppress("UNCHECKED_CAST")
            res.data as List<Transaction>
        } else emptyList()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }

        // 1. Calculate Total Spent from all expenses in the cycle
        totalSpent = expenses.sumOf { it.amount }

        // 2. Group expenses by category for pie chart visualization
        pieChartData = expenses.groupBy { it.category.name }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Top section: Dual-circle overview of Allowance and Spending
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCircle(
                title = "Allowance",
                amount = totalAllowance - totalSpent,
                currencySymbol = currencySymbol,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(150.dp)
            )
            StatCircle(
                title = "Total Spent",
                amount = totalSpent,
                currencySymbol = currencySymbol,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Categorical breakdown pie chart
        RenderPieChart(data = pieChartData)
    }
}

/**
 * A circular indicator displaying a specific budget metric.
 *
 * @param title The label for the metric (e.g., "Allowance").
 * @param amount The numerical value to display.
 * @param currencySymbol The currency symbol to append to the amount.
 * @param color The text color for the amount.
 */
@Composable
fun StatCircle(
    title: String,
    amount: Double,
    currencySymbol: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Displays the formatted amount with the currency symbol
            Text(
                text = "$amount $currencySymbol",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1
            )
        }
    }
}
