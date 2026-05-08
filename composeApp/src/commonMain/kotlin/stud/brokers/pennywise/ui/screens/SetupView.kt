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

/**
 * A screen for initializing a new budget cycle.
 * Prompts the user to enter a total budget and select the start and end dates.
 *
 * @param budgetController The controller for budget-related operations.
 * @param currencySymbol The current currency symbol used in the app.
 * @param onSetupComplete Callback invoked when the budget cycle has been successfully initialized.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupView(budgetController: BudgetController, currencySymbol: String, onSetupComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var totalBudget by remember { mutableStateOf("") }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today.plus(30, DateTimeUnit.DAY)) }

    // State for which date we are currently picking
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    // Dialog for picking dates using Material 3 DatePicker
    if (pickingStartDate || pickingEndDate) {
        val initialDate = if (pickingStartDate) startDate else endDate
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )

        DatePickerDialog(
            onDismissRequest = {
                pickingStartDate = false
                pickingEndDate = false
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        if (pickingStartDate) startDate = selectedDate else endDate = selectedDate
                    }
                    pickingStartDate = false
                    pickingEndDate = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Initialize Budget", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(32.dp))

        // Total budget amount input field
        OutlinedTextField(
            value = totalBudget,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) totalBudget = it },
            label = { Text("Total Budget ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start and End date selection buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { pickingStartDate = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Start Date", style = MaterialTheme.typography.labelSmall)
                    Text(startDate.toString())
                }
            }
            OutlinedButton(
                onClick = { pickingEndDate = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("End Date", style = MaterialTheme.typography.labelSmall)
                    Text(endDate.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Validation for the entered amount and selected dates
        val amount = totalBudget.toDoubleOrNull() ?: 0.0
        val isValid = amount > 0 && endDate > startDate

        Button(
            onClick = {
                if (isValid) {
                    scope.launch {
                        budgetController.initCycle(amount, startDate, endDate)
                        onSetupComplete()
                    }
                }
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
        
        // Inline validation error message
        if (!isValid && totalBudget.isNotEmpty()) {
            Text(
                text = if (amount <= 0) "Amount must be greater than 0" else "End date must be after start date",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
