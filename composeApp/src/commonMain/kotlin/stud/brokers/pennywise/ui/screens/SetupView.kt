package stud.brokers.pennywise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import stud.brokers.pennywise.controllers.BudgetController

@Composable
fun SetupView(budgetController: BudgetController, onSetupComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var totalBudget by remember { mutableStateOf("") }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today.plus(30, DateTimeUnit.DAY)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Initialize Budget", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = totalBudget,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) totalBudget = it },
            label = { Text("Total Budget (EGP)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Simple Date Selection (In a real app, use a DatePicker dialog)
        Button(
            onClick = { /* Trigger DatePicker for End Date */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Date: $endDate")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val amount = totalBudget.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    scope.launch {
                        budgetController.initCycle(amount, startDate, endDate)
                        onSetupComplete()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
