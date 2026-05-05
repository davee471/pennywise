package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardView(
    dailyLimit: Double,
    currencySymbol: String,
    isFinalDay: Boolean,
    isLowBudget: Boolean,
    pieChartData: Map<String, Double>,
    onLogExpenseClick: () -> Unit,
    onLogIncomeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DisplayLimit(
            amount = dailyLimit,
            currencySymbol = currencySymbol,
            isFinalDay = isFinalDay,
            isLowBudget = isLowBudget
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onLogExpenseClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("LOG EXPENSE")
            }

            Button(
                onClick = onLogIncomeClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("LOG INCOME")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        RenderPieChart(data = pieChartData)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayLimit(
    amount: Double,
    currencySymbol: String,
    isFinalDay: Boolean,
    isLowBudget: Boolean
) {
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
            Text("Today's Limit", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$amount $currencySymbol",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                // Display limit in orange if remaining budget is below 20%
                color = if (isLowBudget) Color(0xFFFFA500) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isFinalDay) {
                Spacer(modifier = Modifier.height(8.dp))
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("Final Day") // Final Day warning badge [cite: 1408]
                }
            }
        }
    }
}

@Composable
fun RenderPieChart(data: Map<String, Double>) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No data available. Log an expense to see your insights.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = data.values.sum()
    val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            var startAngle = 0f
            data.values.forEachIndexed { index, value ->
                val sweepAngle = (value.toFloat() / total.toFloat()) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.keys.forEachIndexed { index, category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[index % colors.size], shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(category, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}